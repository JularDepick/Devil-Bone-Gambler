# Gameplay Manual

## What this is

Devil Bone Gambler lets two dice decide: every draw can hand out a reward or a punishment

## The two dice

- **Devil's Dice**: right click to draw; right click inside an altar range opens the offering screen; holding it shows the altars around you through walls
- **Gambler's Dice**: right click to draw; sneak and right click with another player nearby starts a wager instead; holding it shows the dealers around you through walls
- A fallen dealer drops gambler dice according to how well it was fed, and leaves nothing when it starves
- Every draw spends one dice, so a hand without dice cannot draw
- A dice cannot be used again while the previous draw is still settling, and nothing is spent in that case; a draw that fails hands the dice back

## Point, colour and luck

- The point decides which prize region the draw comes from, and a higher region number holds the better entries
- A point above the region number leaves an overflow, and the higher the overflow the more often an uncommon entry of that region comes up
- The colour comes from the luck value: a red point only draws red entries and a black point only black entries
- Red entries tend to be positive and black entries tend to be negative, so the colour alone already tells roughly what is coming
- The luck value is fixed at its base value for now, the full luck gameplay is still being designed

## Devil's Altar

- An altar can be placed on the ground or worn on the head, and wearing it counts as placing one under the wearer
- Players inside the range keep receiving the vanilla strength effect; the higher the offering value of the altar, the larger the range and the higher the strength level
- Carrying an altar in the inventory also grants that strength, but for a shorter time than standing in a range
- Only the strongest altar counts: carrying several altars, or standing in a range while wearing one, never adds up
- Two altar ranges may never overlap; placing an altar inside the range of somebody else is undone, and the notice says whether the range came from a placed altar, from a player wearing one, or from both at once
- Wearing an altar and walking into the range of somebody else keeps hurting the wearer, who is told to leave

### The offering screen

- Right click the Devil's Dice inside the range to open it; right clicking outside every range only draws
- The grid on the top holds the items to offer, the inventory below is the real inventory, and the items really leave it
- The list on the left holds the creatures inside the range, and picking them counts their health as an offering
- Items and mobs of this mod cannot be offered: they do not go into the grid and never appear in the mob list
- The sliders on the right choose the health and the food to offer, and only the box next to each one makes that value count
- Pulling the health slider to the top stakes the whole health bar, and only then does the option to offer the inventory on death show up; a world that keeps the inventory on death never offers it
- The more valuable the offering, the more the range and the level of the altar grow, and the chat reports the settled value after the submission

## Offering Value Table

- Hold the Offering Value Table and right click it to open a searchable table of the values of items and creatures
- The button on the top cycles all, offering values and creature values, and the search box matches both names and registry names
- Resting the pointer on a row shows the registry name of the entry, and an entry that is not registered is drawn in a muted colour
- The value is the basis of the offering conversion: the same item with another damage value or another set of enchantments converts into another value

## Wager

- Sneak and right click the Gambler's Dice to start a wager, and only one wager can run at a time; players nearby join by sneaking twice in a row
- When the countdown ends the dice fall and the point is announced, and the point picks one member
- The dice first draw one outcome out of the region the point stands for, and that outcome lands on the selected member, which can help them or hurt them
- Whoever starts a wager is a member from the beginning, and the wager stops when that player leaves or dies
- A member cannot use the Gambler's Dice until they leave the wager or the wager ends

## Gambler's Dealer

- Gambler's Dealers show up in the world and keep walking around inside their own territory
- Right click a dealer with an empty hand to open the dealer page, a nine grid laid around the dealer with one member in every cell
- The cell in the middle is the dealer itself, and the two bars under its portrait are its health and its food
- Creating a dealer wager spends one Gambler's Dice, and a recruiting dealer can be joined by an empty hand right click; whoever started it can start it early with the button
- The first seconds of the recruit and its last seconds carry a chime, and the countdown between them is quiet
- Clicking your own cell opens your own bet box to put in and take out freely, and clicking another cell only shows that box, where nothing can be taken out or put in
- When the recruit ends the countdown starts, members outside the range are pulled back to the dealer at once, and a member without a stake is removed from this wager
- The point picks one member and the colour decides the payout: a red result takes every stake plus one more copy of the own stake, a black result takes every stake only
- Leaving closes the bet box and returns the stake, and a wager with too few members or without the one who started it is cancelled with every stake returned
- When a wager settles or is cancelled the dealer page closes by itself instead of staying behind
- A dealer has health and food, and while either of them is not full it picks up nearby drops of the Gambler's Dice and refills both at once
- A dealer treats its surroundings as its territory and fights every other dealer that shows up there until one of them dies
- A dealer speaks with the village voice, and a hit or a death is heard as well
- Using the Gambler's Dice keeps adding up, and once enough is gathered a Gambler's Dealer spawn egg arrives

## Crafting

- The only craftable items of the mod are the two dice, the Devil's Altar and the Offering Value Table; the three manuals have no recipe and come with a role
- Devil's Dice: four per craft, with netherrack in the four corners, obsidian on the four edge middles and redstone in the centre
- Gambler's Dice: eight per craft, with leather in the four corners, any planks on the four edge middles and redstone in the centre
- Devil's Altar: one per craft, with glowstone dust, redstone and glowstone dust on the first row, netherrack, obsidian and netherrack on the second, and three obsidian on the third
- Offering Value Table: one per craft, a vertical pair with a book on top and netherrack below

## The manual and the prompts

- Every prompt of the mod appears in the chat box only, the area above the hotbar stays free
- The gameplay manual can be stored in a chest or thrown away, and a missing one is handed out again on the next join

## Good to know

- Items inside a bet box are not yours before the wager settles, so think before putting them in
- Altar ranges may never overlap, so look around and see whose altar you are standing in