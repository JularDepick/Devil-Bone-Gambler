# -*- coding: utf-8 -*-
"""
生成赌徒的荷官实体贴图, 并导出模型几何清单。

贴图为 64x64 的 32 位带透明通道 PNG, 全部像素只取自固定调色板, 不使用抗锯齿与渐变。
贴图按每个长方体的盒式 UV 展开逐像素绘制, 绘制方式是把贴图像素反算回部件局部坐标后
再按设计规则取色, 因此正面的五官与侧面, 顶面的过渡天然连续。

用法:
    python tools/build_dealer_texture.py            # 生成贴图与几何清单
    python tools/build_dealer_texture.py --print     # 额外打印 ASCII 自检图
"""

import argparse
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
OUT_TEXTURE = ROOT / "src" / "main" / "resources" / "assets" / "devil_bone_gambler" / "textures" / "entity" / "gamblers_dealer.png"

# 贴图分辨率: 64 的整数倍且为 64x64, 尺寸理由见 design.md
TEX_W = 64
TEX_H = 64

# ---------------------------------------------------------------- 调色板
# 全部颜色按用途分组, 明暗用同一色系的深浅档位表现, 不引入渐变
PALETTE = {
    "suit_base": (0x3A, 0x3E, 0x49, 255),   # 马甲与短袖主色
    "suit_dark": (0x24, 0x27, 0x2E, 255),   # 马甲暗档, 用于开襟边与下摆
    "suit_lite": (0x4A, 0x4E, 0x5B, 255),   # 马甲亮档, 用于顶面提亮
    "suit_pin": (0x50, 0x55, 0x62, 255),    # 马甲细条缝
    "shirt": (0xEF, 0xEA, 0xDD, 255),       # 衬衫与袖口白
    "shirt_lo": (0xCF, 0xC8, 0xB8, 255),    # 衬衫暗档
    "tie": (0x8A, 0x2B, 0x36, 255),         # 领结酒红
    "tie_dark": (0x62, 0x1C, 0x25, 255),    # 领结暗档
    "pants": (0x2A, 0x2D, 0x35, 255),       # 长裤
    "pants_lo": (0x1C, 0x1F, 0x25, 255),    # 长裤暗档
    "shoe": (0x12, 0x11, 0x10, 255),        # 皮鞋
    "shoe_hi": (0x3A, 0x35, 0x30, 255),     # 皮鞋亮档
    "skin": (0xE2, 0xAF, 0x8A, 255),        # 皮肤
    "skin_lo": (0xC2, 0x8C, 0x69, 255),     # 皮肤暗档
    "skin_hi": (0xF2, 0xC8, 0xA6, 255),     # 皮肤亮档
    "hair": (0x36, 0x25, 0x1B, 255),        # 头发与眉毛
    "hair_hi": (0x4A, 0x33, 0x25, 255),     # 头发亮档
    "stud": (0xE8, 0xE4, 0xD8, 255),        # 纽扣
    "badge": (0xA8, 0x82, 0x46, 255),       # 名牌底色
    "eye_iris": (0x35, 0x4A, 0x6B, 255),    # 眼睛虹膜
    "eye_white": (0xF6, 0xF4, 0xEE, 255),   # 眼白
}

CLEAR = (0, 0, 0, 0)

# ---------------------------------------------------------------- 几何
# 每个长方体: 名称, 父级, 位置, 尺寸, UV 起点
# 这里的"位置"是绘制设计坐标, 只用于把贴图像素反算成取色坐标: 刚性部件取方盒在模型空间的绝对坐标,
# 四肢取方盒自身的局部坐标(腿的 y 从 0 起, 臂的 y 从 0 起), 因此它与模型骨架的挂点不是一回事。
# 骨架的挂点(肩与髋)定义在 client/model/GamblersDealerModel.java, 由 scripts/check_dealer_model.py 校验契约。
CUBOIDS = [
    # 名称, 父级, (x, y, z), (w, h, d), (u, v)
    # UV 排布由 tools/check_dealer_model.py 校验, 要求每个面的矩形不越界且互不重叠
    ("head", "root", (-4.0, -8.0, -4.0), (8, 8, 8), (0, 0)),
    ("nose", "head", (-1.0, -6.0, -5.0), (2, 2, 1), (0, 0)),
    ("fringe", "head", (-4.5, -8.5, -4.35), (9, 3, 1), (24, 0)),
    ("body", "root", (-4.0, 0.0, -2.0), (8, 12, 4), (0, 16)),
    ("hair", "head", (-4.5, -8.6, -4.5), (9, 3, 9), (23, 7)),
    ("right_leg", "root", (-4.0, 0.0, -2.0), (4, 13, 4), (24, 19)),
    ("left_leg", "root", (0.0, 0.0, -2.0), (4, 13, 4), (40, 19)),
    ("right_arm", "body", (-8.0, -2.0, -2.0), (4, 12, 4), (0, 32)),
    ("left_arm", "body", (4.0, -2.0, -2.0), (4, 12, 4), (16, 36)),
]


def box_face_rects(u, v, w, h, d):
    """返回 6 个面在贴图上的矩形 (x, y, w, h), 键为面名。"""
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "west": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "east": (u + d + w, v + d, d, h),
        "back": (u + d + d + w, v + d, w, h),
    }


def local_from_texel(part, face, i, j):
    """把某个面上的贴图像素反算成部件局部坐标, 取像素中心。"""
    x, y, z = part["pos"]
    w, h, d = part["size"]
    cx, cy, cz = i + 0.5, j + 0.5, 0.5
    if face == "front":
        return (x + cx, y + cy, z)
    if face == "back":
        return (x + w - cx, y + cy, z + d)
    if face == "west":
        return (x, y + cy, z + cx)
    if face == "east":
        return (x + w, y + cy, z + d - cx)
    if face == "top":
        return (x + cx, y, z + d - cy)
    if face == "bottom":
        return (x + w - cx, y + h, z + cy)
    raise ValueError("未知面: " + face)


# ---------------------------------------------------------------- 取色规则
def face_shade(color, face, top=None, bottom=None):
    """按面方向取明暗档, 顶面提亮, 底面压暗, 其余保持主色。"""
    if face == "top" and top is not None:
        return PALETTE[top]
    if face == "bottom" and bottom is not None:
        return PALETTE[bottom]
    return PALETTE[color]


def head_color(local, face):
    """头部: 正面是脸, 两侧与背面是头发与肤色, 顶面由发块覆盖。"""
    x, y, z = local
    if x < -4.0 or x > 4.0:
        # 落在贴图接缝上的像素, 收到正面最外侧
        x = -4.0 if x < -4.0 else 4.0
    if face == "back":
        # 后脑: 上半略亮形成明暗层次, 下半为发色
        return PALETTE["hair_hi"] if y < -4.0 else PALETTE["hair"]
    if face in ("west", "east"):
        # 侧面: 靠前的一半是脸, 靠后是头发, 与正面连续
        if z < -1.2:
            front = head_face_color(x, y)
            return front if front is not None else PALETTE["skin"]
        return PALETTE["hair_hi"] if y < -5.0 else PALETTE["hair"]
    if face == "top":
        return PALETTE["hair"]
    return head_face_color(x, y) or PALETTE["skin"]


def head_face_color(x, y):
    """脸部特征: 从额头到下颌的自上而下顺序。返回 None 表示普通皮肤。"""
    # 头发帘遮挡区由发块盖住, 这里只画露出的部分
    if y < -7.2:
        return PALETTE["hair"]
    # 眉毛
    if -6.6 <= y < -6.0 and (-3.0 <= x <= -1.0 or 1.0 <= x <= 3.0):
        return PALETTE["hair"]
    # 眼睛: 2 像素宽, 外侧 1 像素为眼白
    if -6.0 <= y < -4.0:
        if -3.0 <= x < -1.0 or 1.0 <= x < 3.0:
            outer = (x < -2.0) if x < 0 else (x > 2.0)
            if outer:
                return PALETTE["eye_white"]
            return PALETTE["eye_iris"]
    # 鼻子下方的阴影与嘴
    if -4.0 <= y < -3.0 and -1.0 <= x < 1.0:
        return PALETTE["skin_lo"]
    if -3.0 <= y < -2.0 and -2.0 <= x < 2.0:
        return PALETTE["skin_lo"]
    # 下颌阴影
    if y >= -1.0:
        return PALETTE["skin_lo"]
    return None


def body_color(local, face):
    """躯干: 纯色衬衫, 正面画马甲开襟, 白衬衫 V 领, 领结与纽扣与名牌。"""
    x, y, z = local
    w = 8.0
    if x < -4.0 or x > 4.0:
        x = -4.0 if x < -4.0 else 4.0
    ax = x + 4.0  # 0 到 8 的横向位置

    if face == "top":
        return PALETTE["suit_lite"]
    if face == "bottom":
        return PALETTE["suit_dark"]

    if face == "back":
        # 背面是整片马甲, 中缝与下摆用暗档
        if 3.6 <= ax < 4.4 or y > 10.4:
            return PALETTE["suit_dark"]
        return PALETTE["suit_base"]

    if face in ("west", "east"):
        # 两侧仍是马甲, 下摆压暗一档, 靠近前襟处留一条暗边
        if y > 10.4 or z < -1.6:
            return PALETTE["suit_dark"]
        return PALETTE["suit_base"]

    # 正面自上而下依次是白领口, 领结, 门襟, 纽扣与名牌
    # 白衬衫领口
    if y < 1.2:
        if 2.4 <= ax < 5.6:
            return PALETTE["shirt"]
        return PALETTE["suit_base"]
    # 领结: 横向 2 像素, 两侧各一像素深色结翼
    if 1.2 <= y < 2.8 and 2.2 <= ax < 5.8:
        return PALETTE["tie_dark"] if (ax < 3.2 or ax >= 4.8) else PALETTE["tie"]
    # 门襟: 中间露出白衬衫, 下半压暗一档形成阴影
    if 3.2 <= ax < 4.8:
        return PALETTE["shirt"] if y < 8.4 else PALETTE["shirt_lo"]
    # 开襟外侧的暗边
    if 2.8 <= ax < 3.2 or 4.8 <= ax < 5.2:
        return PALETTE["suit_dark"]
    # 名牌: 佩戴者左侧胸口的一小块金色
    if 7.4 <= y < 9.0 and 5.4 <= ax < 6.6:
        return PALETTE["badge"]
    # 纽扣: 佩戴者右侧襟上两枚
    if (4.4 <= y < 5.6 or 7.4 <= y < 8.6) and 1.2 <= ax < 2.0:
        return PALETTE["stud"]
    # 马甲细条缝: 左右襟各一条浅线
    if 0.6 <= ax < 1.0 or 6.0 <= ax < 6.4:
        return PALETTE["suit_pin"]
    return face_shade("suit_base", face, top="suit_lite", bottom="suit_dark")


def arm_color(local, face, side):
    """
    手臂: 纵向 0 到 12 依次是短袖 4 像素, 卷起的白袖口 2 像素, 前臂皮肤 4 像素, 手 2 像素。
    手臂方盒从 y = -2 延伸到 y = -14, 因此纵向位置直接用 s = 0 - y - 2 换算。
    """
    x, y, z = local
    s = 0.0 - y - 2.0
    if s < 4.0:
        return face_shade("suit_base", face, top="suit_lite", bottom="suit_dark")
    if s < 6.0:
        return face_shade("shirt", face, top="shirt", bottom="shirt_lo")
    if s >= 10.0:
        # 手比前臂暗一档, 让手掌在皮肤底色上仍可分辨
        return face_shade("skin_lo", face, top="skin", bottom="skin_lo")
    return face_shade("skin", face, top="skin_hi", bottom="skin_lo")


def leg_color(local, face):
    """腿: 长裤占 11 像素, 最下 2 像素是皮鞋。"""
    x, y, z = local
    if y >= 11.0:
        return face_shade("shoe", face, top="shoe_hi", bottom="shoe")
    # 裤腿中缝的暗线, 仅在正面与背面可见
    if face in ("front", "back") and -0.4 <= x < 0.4:
        return PALETTE["pants_lo"]
    return face_shade("pants", face, top="pants", bottom="pants_lo")


def hair_color(local, face):
    """头发块: 顶面略亮, 其余为发色。"""
    if face == "top":
        return PALETTE["hair_hi"]
    return PALETTE["hair"]


def fringe_color(local, face):
    """刘海: 顶面亮档, 其余发色, 形成发际线的明暗分界。"""
    if face == "top":
        return PALETTE["hair_hi"]
    return PALETTE["hair"]


def nose_color(local, face):
    """鼻子: 用皮肤与暗档勾出轮廓。"""
    if face == "bottom":
        return PALETTE["skin_lo"]
    if face in ("west", "east"):
        return PALETTE["skin_lo"]
    return PALETTE["skin_hi"]


PAINTERS = {
    "head": head_color,
    "hair": hair_color,
    "fringe": fringe_color,
    "nose": nose_color,
    "body": body_color,
    "right_arm": lambda l, f: arm_color(l, f, "right"),
    "left_arm": lambda l, f: arm_color(l, f, "left"),
    "right_leg": leg_color,
    "left_leg": leg_color,
}


def build_texture():
    img = Image.new("RGBA", (TEX_W, TEX_H), CLEAR)
    px = img.load()
    coverage = {}
    for cub in CUBOIDS:
        name, parent, pos, size, uv = cub
        part = {"name": name, "pos": pos, "size": size, "uv": uv}
        rects = box_face_rects(uv[0], uv[1], size[0], size[1], size[2])
        painter = PAINTERS[name]
        covered = 0
        for face, (rx, ry, rw, rh) in sorted(rects.items()):
            for j in range(rh):
                for i in range(rw):
                    local = local_from_texel(part, face, i, j)
                    color = painter(local, face)
                    px[rx + i, ry + j] = color
                    covered += 1
        coverage[name] = {
            "faces": {f: list(r) for f, r in sorted(rects.items())},
            "painted": covered,
        }
    return img, coverage


def main():
    parser = argparse.ArgumentParser(description="生成赌徒的荷官实体贴图")
    parser.add_argument("--print", dest="do_print", action="store_true", help="额外打印 ASCII 自检图")
    args = parser.parse_args()

    img, coverage = build_texture()
    OUT_TEXTURE.parent.mkdir(parents=True, exist_ok=True)
    img.save(OUT_TEXTURE, "PNG")

    alpha_hist = img.getchannel("A").histogram()
    opaque = TEX_W * TEX_H - alpha_hist[0]
    print("贴图已生成: " + str(OUT_TEXTURE))
    print("尺寸: %d x %d" % (TEX_W, TEX_H))
    print("不透明像素: %d / %d" % (opaque, TEX_W * TEX_H))
    print("调色板颜色数: %d" % len(PALETTE))

    if args.do_print:
        print("")
        print_ascii(img)
    return 0


def glyph(p):
    if p[3] < 32:
        return " "
    lum = (p[0] * 299 + p[1] * 587 + p[2] * 114) // 1000
    return " .:-=+*#%@"[lum * 10 // 256]


def print_ascii(img):
    px = img.load()
    print("     " + "".join(str(c // 10) for c in range(TEX_W)))
    print("     " + "".join(str(c % 10) for c in range(TEX_W)))
    for y in range(TEX_H):
        print("%3d| " % y + "".join(glyph(px[x, y]) for x in range(TEX_W)) + " |")


if __name__ == "__main__":
    sys.exit(main())