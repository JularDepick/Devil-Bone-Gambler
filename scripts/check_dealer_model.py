# -*- coding: utf-8 -*-
"""
赌徒的荷官模型与贴图自检脚本。

检查项:
    1. 贴图尺寸, 是否为 16 的整数倍, 位深与是否带透明通道, 是否抗锯齿或渐变
    2. 全部像素是否只取自固定调色板
    3. 每个长方体的 UV 区域是否越界或互相重叠
    4. Java 模型类的几何与 Python 侧的几何清单是否逐项一致
    5. 用纯 Python 软件光栅化把模型渲染成 ASCII 图, 供无图环境核对形状与分区

用法:
    python tools/check_dealer_model.py
"""

import io
import math
import re
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(Path(__file__).resolve().parent))

import build_dealer_texture as B

TEXTURE = ROOT / "src" / "main" / "resources" / "assets" / "devil_bone_gambler" / "textures" / "entity" / "gamblers_dealer.png"
MODEL_JAVA = ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "client" / "model" / "GamblersDealerModel.java"

FAILURES = []
CHECKS = []


def check(name, ok, detail=""):
    CHECKS.append((name, ok, detail))
    if not ok:
        FAILURES.append(name + ": " + detail)
    print("[%s] %s%s" % ("通过" if ok else "失败", name, (" -- " + detail) if detail else ""))


# ------------------------------------------------------------------ 1. 贴图属性
def check_texture_properties(img):
    w, h = img.size
    check("贴图尺寸为 16 的整数倍", w % 16 == 0 and h % 16 == 0, "%dx%d" % (w, h))
    check("贴图尺寸与脚本常量一致", (w, h) == (B.TEX_W, B.TEX_H), "%dx%d" % (w, h))
    check("贴图为 32 位 RGBA", img.mode == "RGBA", img.mode)

    px = img.load()
    alphas = set()
    for y in range(h):
        for x in range(w):
            alphas.add(px[x, y][3])
    check("透明通道只有全透明与全不透明两档", alphas <= {0, 255}, "取值 " + repr(sorted(alphas)))

    # 抗锯齿与渐变检测: 同一部件的同一面上不应出现调色板之外的颜色, 该点由调色板检查覆盖;
    # 这里额外检测是否存在半透明像素与同面对内的孤立插值色
    palette_set = set(tuple(v) for v in B.PALETTE.values())
    palette_set.add(tuple(B.CLEAR))
    bad = []
    for y in range(h):
        for x in range(w):
            if px[x, y] not in palette_set:
                bad.append((x, y, px[x, y]))
    check("全部像素取自固定调色板", not bad, ("越界像素 %d 个, 首个 %s" % (len(bad), bad[0])) if bad else "%d 色" % len(palette_set))


# ------------------------------------------------------------------ 2. UV 区域
def check_uv_regions():
    owner = {}
    cross_conflicts = []
    for cub in B.CUBOIDS:
        name, parent, pos, size, uv = cub
        rects = B.box_face_rects(uv[0], uv[1], size[0], size[1], size[2])
        for face, (rx, ry, rw, rh) in rects.items():
            check("面在贴图内 %s.%s" % (name, face),
                  rx >= 0 and ry >= 0 and rx + rw <= B.TEX_W and ry + rh <= B.TEX_H,
                  "(%d,%d)-(%d,%d)" % (rx, ry, rx + rw - 1, ry + rh - 1))
        for face, (rx, ry, rw, rh) in rects.items():
            for j in range(rh):
                for i in range(rw):
                    key = (rx + i, ry + j)
                    if key in owner and owner[key] != name:
                        cross_conflicts.append((key, owner[key], name))
                    owner[key] = name

    check("不同长方体的贴图像素不冲突", not cross_conflicts,
          repr(cross_conflicts[:5]) if cross_conflicts else "%d 个像素被占用" % len(owner))

    for cub in B.CUBOIDS:
        name, parent, pos, size, uv = cub
        rects = B.box_face_rects(uv[0], uv[1], size[0], size[1], size[2])
        cells = {}
        dup = []
        for face, (rx, ry, rw, rh) in rects.items():
            for j in range(rh):
                for i in range(rw):
                    key = (rx + i, ry + j)
                    if key in cells:
                        dup.append((key, cells[key], face))
                    cells[key] = face
        check("六个面互不重叠 " + name, not dup, repr(dup[:3]) if dup else "%d 像素" % len(cells))


# ------------------------------------------------------------------ 3. Java 与 Python 一致
JAVA_BOX = re.compile(
    r'addOrReplaceChild\(\s*"([a-z_]+)"\s*,\s*CubeListBuilder\.create\(\)\.texOffs\(\s*(-?\d+)\s*,\s*(-?\d+)\s*\)\s*'
    r'\.addBox\(\s*(-?\d+(?:\.\d+)?)F\s*,\s*(-?\d+(?:\.\d+)?)F\s*,\s*(-?\d+(?:\.\d+)?)F\s*,'
    r'\s*(-?\d+(?:\.\d+)?)F\s*,\s*(-?\d+(?:\.\d+)?)F\s*,\s*(-?\d+(?:\.\d+)?)F\s*\)',
    re.S)


def parse_java_model():
    text = MODEL_JAVA.read_text(encoding="utf-8")
    found = {}
    for m in JAVA_BOX.finditer(text):
        name = m.group(1)
        uv = (int(m.group(2)), int(m.group(3)))
        box = tuple(float(m.group(k)) for k in range(4, 10))
        found[name] = {"uv": uv, "box": box}
    return found


# 设计坐标到方盒局部坐标的偏移: 四肢挂在肩与髋的挂点之下, 刚性部件挂在原点。
# 设计坐标本身是绘制用的, 与骨架的挂点无关, 因此比较方盒时需要先补上这个偏移。
DESIGN_TO_BOX = {
    "right_arm": (5.0, 0.0, 0.0),
    "left_arm": (-5.0, 0.0, 0.0),
    "right_leg": (2.0, 0.0, 0.0),
    "left_leg": (-2.0, 0.0, 0.0),
}

# 骨架契约: 肩与髋必须落在原版人形的位置, 否则继承来的动画会绕模型原点摆动。
PIVOT_CONTRACT = {
    "right_arm": (-5.0, 2.0, 0.0),
    "left_arm": (5.0, 2.0, 0.0),
    "right_leg": (-1.9, 12.0, 0.0),
    "left_leg": (1.9, 12.0, 0.0),
}

POSE_OFFSET = re.compile(r'PartPose\.offset\(\s*(-?\d+(?:\.\d+)?)F\s*,\s*(-?\d+(?:\.\d+)?)F\s*,'
                         r'\s*(-?\d+(?:\.\d+)?)F\s*\)')
POSE_ZERO = re.compile(r'PartPose\.ZERO')


def parse_java_poses():
    text = MODEL_JAVA.read_text(encoding="utf-8")
    poses = {}
    for m in re.finditer(r'addOrReplaceChild\(\s*"([a-z_]+)"', text):
        name = m.group(1)
        window = text[m.end():m.end() + 400]
        offset = POSE_OFFSET.search(window)
        if offset:
            poses[name] = tuple(float(offset.group(k)) for k in (1, 2, 3))
        elif POSE_ZERO.search(window):
            poses[name] = (0.0, 0.0, 0.0)
    return poses


def check_java_matches():
    if not MODEL_JAVA.is_file():
        check("Java 模型类存在", False, str(MODEL_JAVA))
        return
    java = parse_java_model()
    poses = parse_java_poses()
    check("Java 模型解析到全部长方体", len(java) == len(B.CUBOIDS),
          "Java %d 个, Python %d 个" % (len(java), len(B.CUBOIDS)))
    check("Java 骨架带 hat 部件", "hat" in poses and poses["hat"] == (0.0, 0.0, 0.0),
          "HumanoidModel 构造时按名字查找 root 下的 hat, 缺失会在创建渲染器时抛异常")
    for name, expected in PIVOT_CONTRACT.items():
        check("挂点符合原版人形 " + name, poses.get(name) == expected,
              "Java %s 期望 %s" % (poses.get(name), expected))
    for cub in B.CUBOIDS:
        name, parent, pos, size, uv = cub
        if name not in java:
            check("Java 含长方体 " + name, False, "缺失")
            continue
        j = java[name]
        shift = DESIGN_TO_BOX.get(name, (0.0, 0.0, 0.0))
        expect_min = tuple(pos[i] + shift[i] for i in range(3))
        ok_uv = j["uv"] == (uv[0], uv[1])
        ok_box = all(abs(j["box"][i] - expect_min[i]) < 1e-6 for i in range(3)) and \
                 all(abs(j["box"][3 + i] - size[i]) < 1e-6 for i in range(3))
        check("Java 与设计表一致 " + name, ok_uv and ok_box,
              "UV Java %s 设计 %s, 盒 Java %s 期望 %s" % (j["uv"], uv, j["box"], tuple(list(expect_min) + list(size))))


def rig_boxes():
    """按 Java 骨架的挂点把方盒换算成模型空间的绝对位置, 供轴测图使用。"""
    java = parse_java_model()
    poses = parse_java_poses()
    boxes = []
    for cub in B.CUBOIDS:
        name, parent, pos, size, uv = cub
        if name in java:
            box = java[name]["box"]
            pose = poses.get(name, (0.0, 0.0, 0.0))
            origin = (pose[0] + box[0], pose[1] + box[1], pose[2] + box[2])
            boxes.append((name, origin[0], origin[1], origin[2], box[3], box[4], box[5], uv[0], uv[1]))
        else:
            boxes.append((name, pos[0], pos[1], pos[2], size[0], size[1], size[2], uv[0], uv[1]))
    return boxes


# ------------------------------------------------------------------ 4. 软件光栅化
def box_face_rects(u, v, w, h, d):
    return B.box_face_rects(u, v, w, h, d)


def cube_quads(x, y, z, w, h, d):
    """顶点顺序为贴图左上, 右上, 右下, 左下, 与 Minecraft 的盒式展开一致。"""
    x0, x1 = x, x + w
    y0, y1 = y, y + h
    z0, z1 = z, z + d
    return [
        ("front", [(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0)]),
        ("back", [(x1, y0, z1), (x0, y0, z1), (x0, y1, z1), (x1, y1, z1)]),
        ("west", [(x0, y0, z0), (x0, y0, z1), (x0, y1, z1), (x0, y1, z0)]),
        ("east", [(x1, y0, z1), (x1, y0, z0), (x1, y1, z0), (x1, y1, z1)]),
        ("top", [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)]),
        ("bottom", [(x1, y1, z0), (x1, y1, z1), (x0, y1, z1), (x0, y1, z0)]),
    ]


def sub(a, b):
    return (a[0] - b[0], a[1] - b[1], a[2] - b[2])


def cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def render(img, boxes, width, height, scale, yaw, pitch=0.0, focus_y=0.0):
    px = img.load()
    zbuf = [[-1e18] * width for _ in range(height)]
    canvas = [[" "] * width for _ in range(height)]
    cyaw, syaw = math.cos(yaw), math.sin(yaw)
    cp, sp = math.cos(pitch), math.sin(pitch)
    cxs, cys = width / 2.0, height / 2.0
    view = (syaw * cp, -sp, cyaw * cp)

    def project(p):
        x, y, z = p
        y -= focus_y
        rx = x * cyaw + z * syaw
        rz = -x * syaw + z * cyaw
        ry = y * cp - rz * sp
        rz2 = y * sp + rz * cp
        return cxs + rx * scale, cys - ry * scale, -rz2

    for name, bx, by, bz, bw, bh, bd, bu, bv in boxes:
        rects = box_face_rects(bu, bv, int(bw), int(bh), int(bd))
        for fname, verts in cube_quads(bx, by, bz, bw, bh, bd):
            n = cross(sub(verts[2], verts[0]), sub(verts[1], verts[0]))
            ln = math.sqrt(sum(c * c for c in n)) or 1.0
            n = (n[0] / ln, n[1] / ln, n[2] / ln)
            if n[0] * view[0] + n[1] * view[1] + n[2] * view[2] >= -0.001:
                continue
            rx, ry_, rw, rh = rects[fname]
            uv = [(rx, ry_), (rx + rw, ry_), (rx + rw, ry_ + rh), (rx, ry_ + rh)]
            pts = [project(v) for v in verts]
            minx = max(0, int(math.floor(min(p[0] for p in pts))))
            maxx = min(width - 1, int(math.ceil(max(p[0] for p in pts))))
            miny = max(0, int(math.floor(min(p[1] for p in pts))))
            maxy = min(height - 1, int(math.ceil(max(p[1] for p in pts))))
            x0s, y0s, _ = pts[0]
            x1s, y1s, _ = pts[1]
            x3s, y3s, _ = pts[3]
            det = (x1s - x0s) * (y3s - y0s) - (x3s - x0s) * (y1s - y0s)
            if abs(det) < 1e-9:
                continue
            for pyy in range(miny, maxy + 1):
                for pxx in range(minx, maxx + 1):
                    ux = pxx + 0.5 - x0s
                    uy = pyy + 0.5 - y0s
                    a = (ux * (y3s - y0s) - uy * (x3s - x0s)) / det
                    b = (uy * (x1s - x0s) - ux * (y1s - y0s)) / det
                    if a < -0.001 or b < -0.001 or a + b > 1.001:
                        continue
                    depth = (1 - a - b) * pts[0][2] + a * pts[1][2] + b * pts[3][2]
                    if depth <= zbuf[pyy][pxx]:
                        continue
                    su = (1 - a - b) * uv[0][0] + a * uv[1][0] + b * uv[3][0]
                    sv = (1 - a - b) * uv[0][1] + a * uv[1][1] + b * uv[3][1]
                    uu = int(min(max(su, 0), img.size[0] - 0.001))
                    vv = int(min(max(sv, 0), img.size[1] - 0.001))
                    r, g, bl, al = px[uu, vv]
                    zbuf[pyy][pxx] = depth
                    if al < 32:
                        continue
                    lum = (r * 299 + g * 587 + bl * 114) // 1000
                    canvas[pyy][pxx] = " .:-=+*#%@"[lum * 10 // 256]
    return [("".join(row)).rstrip() for row in canvas]


def render_views(img):
    # 模型整体范围: y 从 -8.6 到 25, 中心约 8.2
    boxes = rig_boxes()
    views = [
        ("正面 (yaw 0)", 0.0),
        ("背面 (yaw 180)", math.pi),
        ("左侧 (yaw -90)", -math.pi / 2),
        ("右侧 (yaw 90)", math.pi / 2),
        ("四分之三 (yaw 35)", math.radians(35)),
    ]
    for label, yaw in views:
        print("")
        print("---- " + label + " ----")
        for row in render(img, boxes, 44, 52, 1.35, yaw, 0.0, 8.2):
            if row:
                print("   " + row)


# 头部动画口径: 头部转向与受击动画全部交给父类, 模型只保留偏航限制, 因此头部不会转到背后或抖动。
def check_head_rule():
    text = MODEL_JAVA.read_text(encoding="utf-8")
    check("头部偏航有限幅", "MAX_HEAD_YAW" in text and "Mth.clamp(this.head.yRot" in text)
    check("模型不做受击抖动动画", "HURT_HEAD_SHAKE" not in text and "Math.sin(entity.hurtTime" not in text)
    check("受击时头部只做阻尼", "HURT_HEAD_DAMPING" in text)
    check("模型不自行驱动待机头部摆动", "IDLE_HEAD" not in text)
    check("头部偏航做了非有限值回退", "Float.isFinite(this.head.yRot)" in text)


def check_attack_cadence():
    constants = (ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "util" / "ModConstants.java").read_text(
        encoding="utf-8")
    found = re.search(r"DEALER_TERRITORY_ATTACK_INTERVAL_TICKS\s*=\s*(\d+)", constants)
    check("领地攻击间隔为一秒", bool(found) and int(found.group(1)) == 20,
          "取值 " + (found.group(1) if found else "缺失"))


# 面板头像与招募提示音口径: 面板头像必须取自荷官贴图的头部 UV, 提示音只在开局与末尾响。
# 音效口径: 荷官使用村民音效, 环境音/受击/死亡三条都必须覆盖。
def check_dealer_habits():
    """The dealer's daily business: paying for swings, eating only hungry and only its own dice, ignoring players while
    it fights, keeping the idle name private and fumbling dice when struck."""
    dealer = (ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "mob" / "GamblersDealer.java").read_text(
        encoding="utf-8")
    constants = (ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "util" / "ModConstants.java").read_text(
        encoding="utf-8")

    check("挥击按百分比扣饱食度", "DEALER_SWING_COST_PERCENT" in dealer
          and "this.satiety - this.maxSatiety() * ModConstants.DEALER_SWING_COST_PERCENT" in dealer)
    check("饱食度为空时不出手", "this.satiety <= 0" in dealer and "return;" in dealer)
    check("只在六成以下才进食", "DEALER_FEED_THRESHOLD" in dealer and "isHungry" in dealer)

    eat = re.search(r"private void tryEat\(\).*?\n    \}", dealer, re.S)
    pickup = re.search(r"private void tryPickup\(\).*?\n    \}", dealer, re.S)
    check("竞争时不吃", eat is not None and "isDefendingTerritory()" in eat.group(0),
          "tryEat 在与别的荷官竞争时仍然会进食")
    check("竞争时不捡", pickup is not None and "isDefendingTerritory()" in pickup.group(0),
          "tryPickup 在与别的荷官竞争时仍然会捡骰子")
    check("每次只捡一个并有冷却", "DEALER_PICKUP_INTERVAL_TICKS = 40" in constants
          and "this.pickupTicks = ModConstants.DEALER_PICKUP_INTERVAL_TICKS" in dealer
          and pickup is not None and pickup.group(0).count("this.pocketDice++") == 1
          and pickup.group(0).count("return;") >= 2, "捡拾一次只应拿走一个骰子")
    check("只吃赌徒的骰子", "ModItems.GAMBLERS_DICE.get()" in dealer and "DEVILS_DICE" not in dealer)
    check("进食冷却为两秒", re.search(r"DEALER_FEED_INTERVAL_TICKS\s*=\s*40", constants) is not None)
    check("竞争中不可交互", "this.rival != null" in dealer and "VILLAGER_NO" in dealer)
    check("追击速度已放慢", re.search(r"DEALER_TERRITORY_CHASE_SPEED\s*=\s*0\.9D", constants) is not None)
    check("待机名只对看向它的玩家显示", "shouldShowName" in dealer and "isLookingAtMe" in dealer)
    check("主持与招募名不受影响", "showNameToEveryone" in dealer)
    check("受伤有几率掉骰子", "dropHurtDice" in dealer and "DEALER_HURT_DROP_CHANCE_ONE" in dealer)
    check("受击掉落合计百分之五", re.search(r"DEALER_HURT_DROP_CHANCE_THREE\s*=\s*0\.01D", constants) is not None
          and re.search(r"DEALER_HURT_DROP_CHANCE_TWO\s*=\s*0\.015D", constants) is not None
          and re.search(r"DEALER_HURT_DROP_CHANCE_ONE\s*=\s*0\.025D", constants) is not None,
          "受击掉落的 1% 掉 3 个 / 1.5% 掉 2 个 / 2.5% 掉 1 个 与文档不一致")
    check("受击掉落每分钟上限", re.search(r"DEALER_HURT_DROP_WINDOW_TICKS\s*=\s*1200", constants) is not None
          and re.search(r"DEALER_HURT_DROP_WINDOW_MAX\s*=\s*6", constants) is not None
          and "DEALER_HURT_DROP_WINDOW_MAX" in dealer
          and "openHurtDropWindow" in dealer,
          "受击掉落的一分钟 6 个上限没有被使用")
    check("受击掉落不影响死亡掉落", "isDeadOrDying()" in dealer)



def check_dealer_glow():
    """The dealer carries half a torch: a light block kept under its feet, and a leftover copy puts itself out."""
    dealer = (ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "mob" / "GamblersDealer.java").read_text(
        encoding="utf-8")
    constants = (ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "util" / "ModConstants.java").read_text(
        encoding="utf-8")
    glow = (ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "block" / "DealerGlowBlock.java")
    blocks = (ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "registry" / "ModBlocks.java").read_text(
        encoding="utf-8")

    check("荷官自带光照强度", glow.exists() and "DEALER_LIGHT_LEVEL" in constants)
    check("亮度为火把的一半", re.search(r"DEALER_LIGHT_LEVEL\s*=\s*7\b", constants) is not None
          and re.search(r"ALTAR_LIGHT_LEVEL\s*=\s*14\b", constants) is not None)
    check("光照块已注册且没有物品", "DEALER_GLOW" in blocks and "BLOCK_ITEMS.register(\"dealer_glow\"" not in blocks)
    check("光照跟着荷官走", "tickGlow" in dealer and "clearGlow" in dealer)
    check("荷官消失时收走光照", "public void remove(Entity.RemovalReason" in dealer and "clearGlow()" in dealer)
    check("遗留的光照会自行熄灭", glow.exists() and "randomTick" in glow.read_text(encoding="utf-8"))


def check_dealer_sounds():
    mob_file = ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "mob" / "GamblersDealer.java"
    text = mob_file.read_text(encoding="utf-8")
    for name in ("VILLAGER_AMBIENT", "VILLAGER_HURT", "VILLAGER_DEATH", "VILLAGER_YES"):
        check("荷官使用村民音效 " + name, name in text)


# 死亡掉落口径: 满饱食度掉 4 个赌徒的骰子, 空饱食度不掉, 中间线性且向下取整。
def check_death_drops():
    mob_file = ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "mob" / "GamblersDealer.java"
    text = mob_file.read_text(encoding="utf-8")
    check("荷官死亡掉落走原版死亡掉落钩子", "dropCustomDeathLoot" in text)
    check("死亡掉落背包里剩余的骰子", "new ItemStack(ModItems.GAMBLERS_DICE.get(), this.pocketDice)" in text)
    check("死亡时不再按饱食度结算", "deathDiceDrops" not in text)


def check_panel_and_chime():
    screen = ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "client" / "gui" / "DealerScreen.java"
    text = screen.read_text(encoding="utf-8")
    check("荷官面板头像取自荷官贴图", "textures/entity/gamblers_dealer.png" in text and "villager.png" not in text)
    check("荷官面板头像按头部 UV 截取", "HEAD_U = 8" in text and "HEAD_V = 8" in text)
    check("荷官面板显示生命与饱食状态条", "HEALTH_COLOR" in text and "SATIETY_COLOR" in text)

    constants = (ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "util" / "ModConstants.java").read_text(
        encoding="utf-8")
    head = re.search(r"DEALER_WAGER_CHIME_HEAD_SECONDS\s*=\s*(\d+)", constants)
    tail = re.search(r"DEALER_WAGER_CHIME_TAIL_SECONDS\s*=\s*(\d+)", constants)
    check("招募提示音只在开局 3 秒与末尾 10 秒", bool(head) and bool(tail) and head.group(1) == "3"
          and tail.group(1) == "10", (head.group(1) if head else "缺失") + "/" + (tail.group(1) if tail else "缺失"))


def check_liquid_escape():
    """A dealer hates liquid: as soon as it stands in water or lava it stops everything else and climbs out."""
    dealer = (ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "mob" / "GamblersDealer.java").read_text(encoding="utf-8")
    wander = (ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "mob" / "DealerWanderGoal.java").read_text(encoding="utf-8")
    constants = (ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "util" / "ModConstants.java").read_text(encoding="utf-8")

    check("液体判定包含水面与岩浆", "isInWaterOrBubble()" in dealer and "isInLava()" in dealer)
    check("液体中立即离开", "isInLiquid()" in dealer and "tickLiquidEscape" in dealer)

    escape = dealer[dealer.index("private void tickLiquidEscape()"):dealer.index("private BlockPos findShore()")]
    check("离开时直接驱动移动控制", "getMoveControl().setWantedPosition" in escape,
          "离开路径必须走移动控制, 地面寻路在水里不可用")
    check("离开时打断旧路径", "getNavigation().stop()" in escape)

    search = dealer[dealer.index("private BlockPos findShore()"):dealer.index("private boolean isDryStand(")]
    check("由近到远找岸边", "DEALER_LIQUID_ESCAPE_RADIUS" in search and "distance++" in search)
    check("岸边不含岩浆与火", "MAGMA_BLOCK" in dealer and "Blocks.FIRE" in dealer and "SOUL_FIRE" in dealer)
    check("岸边需要实地与空位", "isFaceSturdy" in dealer and "getFluidState().isEmpty()" in dealer)

    check("离开半径与重算间隔",
          "DEALER_LIQUID_ESCAPE_RADIUS = 16" in constants and "DEALER_LIQUID_ESCAPE_REPATH_TICKS = 5" in constants)
    check("离开速度高于走路", "DEALER_LIQUID_ESCAPE_SPEED = 1.2D" in constants)

    step = dealer[dealer.index("protected void customServerAiStep()"):]
    step = step[:step.index("this.tickGlow()")]
    check("液体中不谈领地", "isInLiquid()" in step
          and step.index("tickLiquidEscape") < step.index("tickTerritory"))

    eat = dealer[dealer.index("private void tryEat()"):]
    eat = eat[:eat.index("private ", 10)]
    check("液体中不进食", "isInLiquid()" in eat)
    check("液体中不闲逛", "isInLiquid()" in wander)


def check_recruit_text():
    """The recruit name tag names both the members already in and the seconds left."""
    dealer = (ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "mob" / "GamblersDealer.java").read_text(encoding="utf-8")
    service = (ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "dealer" / "DealerWagerService.java").read_text(encoding="utf-8")
    lang = (ROOT / "src" / "main" / "resources" / "assets" / "devil_bone_gambler" / "lang" / "zh_cn.json").read_text(encoding="utf-8")

    check("招募名牌带人数与秒数", "public void showRecruitText(int members, int seconds)" in dealer
          and dealer.count("Integer.valueOf(members)") >= 1 and dealer.count("Integer.valueOf(seconds)") >= 1)
    check("招募名牌随人数刷新", service.count("showRecruitText(session.memberCount()") >= 2)
    check("招募文案有两个占位符", '"gui.devil_bone_gambler.dealer.recruiting": "' in lang
          and lang.split('"gui.devil_bone_gambler.dealer.recruiting": "')[1].split('"')[0].count("%s") == 2)


def check_neutrality():
    """The dealer is neutral: no mob targets it, it only ever attacks dealers, and nothing counts it as a monster."""
    dealer = (ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "mob" / "GamblersDealer.java").read_text(encoding="utf-8")
    entities = (ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "registry" / "ModEntities.java").read_text(encoding="utf-8")

    check("荷官不被当作敌对目标", "public boolean canBeSeenAsEnemy()" in dealer
          and dealer.split("public boolean canBeSeenAsEnemy()")[1].split("}")[0].strip().startswith("{")
          and "return false;" in dealer.split("public boolean canBeSeenAsEnemy()")[1].split("}")[0])
    check("荷官只对荷官动手", "public boolean canAttackType(EntityType<?> type)" in dealer
          and "ModEntities.GAMBLERS_DEALER.get()" in dealer)
    check("荷官不是怪物类型", "extends PathfinderMob" in dealer and "implements" not in dealer.split("extends PathfinderMob")[1].split("{")[0])
    check("荷官注册为生物类别", "MobCategory.CREATURE" in entities)
    check("领地只认荷官", "private GamblersDealer findIntruder()" in dealer)


def check_hurt_condition():
    """Dice are fumbled only when the dealer really loses health, not merely when a hit lands and is shrugged off."""
    dealer = (ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "mob" / "GamblersDealer.java").read_text(encoding="utf-8")
    hurt = dealer[dealer.index("public boolean hurt(DamageSource source, float amount)"):]
    hurt = hurt[:hurt.index("private void dropHurtDice()")]

    check("只有真正掉血才掷骰", "healthBefore" in hurt and "this.getHealth() < healthBefore" in hurt,
          "受击但没掉血的命中不应触发掉落")
    check("死亡掉落与受伤掉落分开", "dropCustomDeathLoot" in dealer and "dropHurtDice" in dealer)


def check_pocket():
    """The dealer pocket: dice in, dice out, experience from the wasted part of a meal and the growth it pays for."""
    dealer = (ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "mob" / "GamblersDealer.java").read_text(encoding="utf-8")
    service = (ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "dealer" / "DealerWagerService.java").read_text(encoding="utf-8")
    constants = (ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "util" / "ModConstants.java").read_text(encoding="utf-8")

    check("背包初始 16 个", "DEALER_POCKET_INITIAL_DICE = 16" in constants
          and "private int pocketDice = ModConstants.DEALER_POCKET_INITIAL_DICE" in dealer)
    check("背包基础容量 32", "DEALER_POCKET_BASE = 32" in constants
          and "DEALER_POCKET_BASE + (int) Math.floor" in dealer)
    check("经验扩背包并提升上限", "DEALER_STAT_PER_EXPERIENCE = 0.1D" in constants
          and "DEALER_STAT_BASE_MULTIPLIER = 2.0D" in constants
          and "gainExperience" in dealer and "applyProgress" in dealer)
    check("一个骰子回一半", "DEALER_DICE_RESTORE_SHARE = 0.5D" in constants
          and "DEALER_DICE_RESTORE_SHARE" in dealer)
    check("溢出转为经验", "0.5D * (overflowHealth / restoreHealth + overflowSatiety / restoreSatiety)" in dealer)
    check("捡起的骰子进入背包", "this.pocketDice++" in dealer)
    check("背包满了不再捡", "this.pocketDice >= this.pocketCapacity()" in dealer)
    check("进食来自背包", "eatFromPocket" in dealer and "this.pocketDice--" in dealer)
    check("受伤掉落从背包扣除", "this.pocketDice -= count" in dealer)
    check("背包空后不掉骰子", "if (this.pocketDice <= 0)" in dealer)
    check("背包进存档", "tag.putInt(TAG_POCKET" in dealer and "tag.putDouble(TAG_EXPERIENCE" in dealer
          and "tag.contains(TAG_POCKET)" in dealer)
    check("赌局骰子先到手上", "dealer.holdWagerDice()" in service and "public void holdWagerDice()" in dealer)
    check("赌局成功后进背包", "dealer.receiveWagerDice()" in service and "public void receiveWagerDice()" in dealer)
    check("背包满时结算后立刻吃掉", service.index("receiveWagerDice") < service.index("consumeForWager")
          and "this.eatOneDice();" in dealer)
    check("取消时取回手上的骰子", "dealer.clearWagerDice()" in service)
    check("主持时手里才有骰子", "this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GAMBLERS_DICE.get()))" in dealer)


def check_bar_economy():
    """Percent based bar costs, the eat first debt rule, the daily drain and the attack that grows with the ceiling."""
    dealer = (ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "mob" / "GamblersDealer.java").read_text(encoding="utf-8")
    constants = (ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "util" / "ModConstants.java").read_text(encoding="utf-8")

    check("主持按百分比扣", "DEALER_WAGER_COST_BASE_PERCENT = 12.0D" in constants
          and "DEALER_WAGER_COST_PER_MEMBER_PERCENT = 1.0D" in constants
          and "DEALER_WAGER_COST_BASE_PERCENT" in dealer)
    method = dealer[dealer.index("public void consumeForWager"):]
    method = method[:method.index("private double paySatiety")]
    check("先扣饱食度再吃骰子再扣血", "paySatiety" in method and "eatFromPocket" in method
          and method.index("eatFromPocket") < method.index("debtPercent"))
    check("饱食度按小数计", "private double satiety" in dealer and "tag.putDouble(TAG_SATIETY" in dealer)
    check("攻击伤害随生命上限", "getMaxHealth() / ModConstants.PLAYER_BASE_MAX_HEALTH" in dealer
          and "DEALER_TERRITORY_MAX_DAMAGE" in dealer)
    check("每二十刻掉一次", "DEALER_DECAY_INTERVAL_TICKS = 20" in constants and "tickSatiety" in dealer)
    check("一昼夜掉一百点", "DEALER_DAY_TICKS" in constants
          and "100.0D * DEALER_DECAY_INTERVAL_TICKS / DEALER_DAY_TICKS" in constants)
    body = dealer[dealer.index("private void tickSatiety"):]
    body = body[:body.index("private void drainHealth")]
    check("主持期间暂停并回复", "if (this.hosting)" in body and "this.satiety + satietyStep" in body
          and "this.getHealth() + healthStep" in body)
    check("饱食度空了扣血", "drainHealth" in body and "DEALER_SATIETY_PERCENT_PER_HEALTH_PERCENT" in body)
    check("主持从创建就开始", "dealer.setHosting(true)" in (ROOT / "src" / "main" / "java" / "com" / "devilbonegambler" / "dealer" / "DealerWagerService.java").read_text(encoding="utf-8"))


def main():
    if not TEXTURE.is_file():
        print("缺少贴图: " + str(TEXTURE))
        return 2

    with Image.open(TEXTURE) as raw:
        img = raw.convert("RGBA")

    check_texture_properties(img)
    check_uv_regions()
    check_java_matches()
    check_head_rule()
    check_attack_cadence()
    check_dealer_habits()
    check_dealer_glow()
    check_panel_and_chime()
    check_dealer_sounds()
    check_death_drops()
    check_pocket()
    check_bar_economy()
    check_liquid_escape()
    check_recruit_text()
    check_neutrality()
    check_hurt_condition()

    print("")
    print("调色板清单:")
    for key in sorted(B.PALETTE):
        v = B.PALETTE[key]
        print("    %-10s #%02X%02X%02X" % (key, v[0], v[1], v[2]))

    print("")
    print("轴测自检图 (渐变色阶: 空格最暗, @ 最亮):")
    render_views(img)

    print("")
    if FAILURES:
        print("自检失败 %d 项:" % len(FAILURES))
        for f in FAILURES:
            print("   " + f)
        return 1
    print("自检全部通过, 共 %d 项" % len(CHECKS))
    return 0


if __name__ == "__main__":
    sys.exit(main())
