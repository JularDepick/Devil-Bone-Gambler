# Gameplay Manual

## The two dice

- **Devil's Dice**: right click to draw; right click inside an altar range opens the offering screen; sneaking with it makes the altars around you glow
- **Gambler's Dice**: right click to draw; sneak and right click with another player nearby starts a wager instead; sneaking with it makes the dealers around you glow
- Every draw spends one dice, so a hand without dice cannot draw
- A dice cannot be used again while the previous draw is still settling, and nothing is spent in that case; a draw that fails hands the dice back

## Drawing rules

- The point decides which prize region the draw comes from, and a higher region number holds the better entries
- A point above the region number leaves an overflow, and the higher the overflow the more often an uncommon entry of that region comes up
- The colour comes from the luck value: a red point draws only red entries and a black point only black entries
- Red entries tend to be positive and black entries tend to be negative, so the colour alone already tells roughly what is coming
- After the roll the point and the colour appear in one line above the hotbar, for example Gambler's Dice 5 points Red; this is the only mod line that uses that area, and the overflow value is not shown there
- The luck value changes over time and is shown in the top right corner, coloured by its band; a higher value leans to red points and rare rewards, a lower one leans to black points and common entries

## Devil's Altar

- An altar can be placed on the ground or worn on the head, and wearing it counts as placing one under the wearer
- Players inside the range keep receiving the vanilla strength effect, and a higher offering value means a larger range and a higher strength level
- Carrying an altar in the inventory grants the same strength, for a shorter time than standing inside a range
- Only the strongest altar counts: several altars, or standing in a range while wearing one, never add up
- Two altar ranges may never overlap; placing one inside somebody else range is undone, and the notice says whether the range comes from a placed altar, from a worn one, or from both at once
- Wearing an altar into somebody else range keeps hurting the wearer, who is told to leave
- Only an iron pickaxe or a better one can take the altar down and make it drop; other tools, explosions, pistons and creatures cannot

### The offering screen

- Right click the Devil's Dice inside a range to open it; right clicking outside every range only draws
- The grid on the top holds the items to offer, the inventory below is the real inventory, and the items really leave it
- The list on the left holds the creatures inside the range, and picking them counts their health as an offering
- Items and mobs of this mod cannot be offered: they do not go into the grid and never appear in the mob list
- The sliders on the right choose the health and the food to offer, and only the box next to each one makes that value count
- Pulling the health slider to the top stakes the whole health bar, and only then does the option to offer the inventory on death show up; a world that keeps the inventory on death never offers it
- The screen closes as soon as the offering is submitted and shows Please wait for the settlement; when it settles, the chat box first reports the converted value of the offering, then the draw result
- The more valuable the offering, the more the range and the level of the altar grow, and the chat reports the settled value after the submission

### Manuals into dice

- Drop at least 8 gameplay manuals inside an altar range and the altar takes them, turning them into the same number of dice
- How many dice of each kind you get is random, and the split differs on every conversion
- Role bound manuals cannot be converted this way

## Offering Value Table

- Hold the table and right click it to open a searchable list of the values of items and creatures
- The button on the top cycles all, offering values and creature values, and the search box matches both names and registry names
- Resting the pointer on a row shows the registry name, and an entry that is not registered is drawn in a muted colour
- The value is the basis of the offering conversion: the same item with another damage value or another set of enchantments converts into another value

## Wager

- Sneak and right click the Gambler's Dice to start a wager, and only one wager can run at a time
- Players nearby join by sneaking twice in a row
- When the countdown ends the dice fall and the point is announced, and the point picks one member
- The dice first draw one outcome out of the region the point stands for, and that outcome lands on the selected member
- Whoever starts a wager is a member from the beginning, and the wager stops when that player leaves or dies
- A member cannot use the Gambler's Dice until they leave the wager or the wager ends

## Gambler's Dealer

- Dealers show up in the world and keep walking inside their own territory; a dealer is neutral, never attacks another kind of creature and is never targeted by monsters, and every other dealer inside its territory is fought until one of them dies
- Right click a dealer with an empty hand to open the dealer page: a nine grid laid around the dealer, one member in every cell, the dealer itself in the middle cell and its health and food under the portrait; hovering the middle cell also shows its experience
- A dealer keeps a pocket that holds nothing but Gambler's Dice; while it is not fighting, it picks up nearby dice as long as the pocket has room
- Dice beat light for a dealer: it walks to nearby dice on the ground, or to the item frame that holds one, and a full pocket only stops the pickup, not the walk
- When its health or food drops to six tenths, the dealer eats dice from the pocket one by one, each restoring half of a bar, and it stops once both are full; it neither picks up nor eats while fighting another dealer
- A dealer that recruits or hosts a wager sits out of the dealer against dealer fights and is neither attacker nor target
- Whatever a meal cannot restore turns into experience: the more experience, the larger the pocket, the higher both ceilings and the harder it hits
- Eating one dice takes a second with it at the mouth, and the dealer waits another second before the next one
- A wound may shake dice out of the pocket and an empty pocket shakes out nothing
- A recruiting, hosting or fighting dealer takes no damage from players at all, and an idle one takes a player hit at most once per second
- A dealer hates liquid: in water or lava it climbs out at once, and while wet it neither fights nor eats
- It dislikes altars on the ground as well: a walk never ends inside an altar range and the route does not cut through one
- Its food drains with time and runs out over a whole day; during a recruit or a wager it stops draining and both bars refill slowly instead, as if the players were feeding it
- A fallen dealer drops the dice still left in its pocket
- A dealer speaks with the village voice, and a hit or a death is heard as well

### Dealer wagers

- Creating a wager takes one Gambler's Dice from the starter, which the dealer holds in its hand; the dice joins the pocket only when the wager settles, and a full pocket means the dealer eats it right away; a cancelled wager returns the dice
- A settled wager costs the dealer part of its food; the dice it just earned pays the bill first and only a remaining part is paid with health
- A recruiting dealer can be joined by an empty hand right click; whoever started it can start it early with the button
- The first seconds of the recruit and its last seconds carry a chime, and the countdown between them is quiet
- While recruiting the dealer shows one line overhead: the wager is being called, with the member count and the seconds left inside the same line
- In the last five seconds the joined players see a one second countdown in chat
- Clicking your own cell opens your own bet box to put in and take out freely, and clicking another cell only shows that box
- The manuals that follow a role cannot be put into a bet box, every other item can
- When the recruit ends the countdown starts: members outside the range are pulled back to the dealer at once, and a member without a stake is removed
- The point picks one member and the colour decides the payout: red takes every stake plus one more copy of the own stake, black takes every stake only
- Leaving closes the bet box and returns the stake; a wager with too few members or without the one who started it is cancelled with every stake returned
- When a wager settles or is cancelled the page closes by itself
- Using the Gambler's Dice keeps adding up, and once enough is gathered a dealer spawn egg arrives

## Crafting

- The only craftable items are the two dice, the Devil's Altar and the Offering Value Table; the three manuals have no recipe and come with a role
- Devil's Dice: four per craft, with netherrack in the four corners, obsidian on the four edge middles and redstone in the centre
- Gambler's Dice: eight per craft, with leather in the four corners, any planks on the four edge middles and redstone in the centre
- Devil's Altar: one per craft, with glowstone dust, redstone and glowstone dust on the first row, netherrack, obsidian and netherrack on the second, and three obsidian on the third
- Offering Value Table: one per craft, a vertical pair with a book on top and netherrack below

## Prompts and the manual

- Every prompt appears in the chat box
- The gameplay manual can be stored in a chest or thrown away, and a missing one is handed out again on the next join

## Good to know

- Items inside a bet box are not yours before the wager settles
- Altar ranges may never overlap, so look around and see whose altar you are standing in
