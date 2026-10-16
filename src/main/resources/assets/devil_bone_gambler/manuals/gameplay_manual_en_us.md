# Gameplay Manual

## What this is

Devil Bone Gambler lets the dice decide: every draw can hand out a reward or a punishment.

## The two dice

- **Devil's Dice**: right click to draw at once; right click inside an altar range to open the offering screen
- **Gambler's Dice**: right click to draw at once; sneak and right click with another player nearby to start a wager, and the gambler's dice is locked for everyone in the wager until it ends

## Points and colours

- The point decides the prize region, and a higher region number holds the better entries
- A point above the region number leaves an overflow, and the higher the overflow the more likely an uncommon entry of that region is drawn
- The colour comes from the luck value: a red point only draws red entries and a black point only black entries
- Red entries tend to be positive, black entries tend to be negative
- The luck value is fixed at its base value for now, its full effect is still being designed

## Devil's Altar

- An altar can be placed on the ground or worn on the head, and wearing it counts as placing it at the wearer
- Right click the Devil's Dice inside an altar range to open the offering screen
- The offering screen: put items into the grid on top, pick the creatures around you from the list on the left, choose the health and the food to offer on the right, and mark the death offering checkbox if wanted
- The more valuable the offering, the larger the altar range and the higher its effect level
- Players inside the range keep receiving the vanilla strength effect
- Two altar ranges may never overlap

## Offering value table

- Right click the Offering Value Table to look up the value of items and creatures, and search it with the box
- Resting the pointer on a row shows the registry name of that entry
- The value is the basis of the offering conversion; damage and enchantments of the very stack change the settled value

## Wager

- Sneak and right click the Gambler's Dice to start a wager, and nearby players join by sneaking twice in a row
- When the countdown ends, one member is selected by the point
- Red result: the selected member takes every bet and one extra copy of the own bet
- Black result: the selected member takes every bet

## Gambler's Dealer

- Gambler's Dealers appear in the world
- Right click a dealer with an empty hand to create or join a dealer wager, and to put your own stake into your bet box
- Using the Gambler's Dice keeps adding progress, and a dealer spawn egg is granted once enough progress is gathered

## Random prize pools

- A prize entry may hold random tokens such as ${random:item} and ${random:entity}
- The server picks one sample out of the matching pool while the entry runs

## Prompts

- Every prompt of the mod appears in the chat box only, the area above the hotbar stays free

## Manuals

- **Gameplay Manual**: the one you are reading
- **Admin Manual**: how to use the configuration panel
- **Creator's Manual**: the creator only notes followed by the admin manual content
