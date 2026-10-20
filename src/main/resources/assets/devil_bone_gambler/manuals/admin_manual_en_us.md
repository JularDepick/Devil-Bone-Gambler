# Admin Manual

## Opening the panel

- Press Shift and D in game, and the key can be rebound in the vanilla controls screen
- Or select this mod in the mod list of the game menu and press its config button, which is exactly the same as Shift and D
- Or run /devilbonegambler config, the short form /dbgr config is equivalent
- Only the creator and the admins may open it, a press by anybody else does nothing

## Panel overview

- The tabs on the top are the prize configuration and the offering values
- The upper part edits the fields and the lower part is the table: the header stays frozen, the columns stay fixed, long content is clipped, and resting the pointer on it shows it in full
- The scroll bar can be dragged, and the status line reports the last action

## Prize pools

- The Pools button in the top right opens the pool list
- Each row is one pool with its name, entry count and whether it is active
- Select a pool to edit its name and description, then press Save and use to switch right away
- New adds a pool with one empty region
- Delete asks twice and the confirm label turns red; deleting the active pool switches to the next one, and deleting the last one creates a blank pool

## Prize configuration

- The columns are the region, the entry name, the weight and the colour
- Adding a region appends one, and the region number matches a dice point; removing a selected region removes its entries as well
- Adding an entry appends one row in the current region, with the fields region number, entry name, weight, colour, major event, enabled, command content and trigger message
- The entry name is a plain string: a preset name is written in the game language when the file is created and never follows a later language change
- A higher weight is drawn more often; red is positive and black is negative
- The colour, the major event and the enabled state are buttons, one click toggles one of them
- The command content may hold several lines, one vanilla command per line, and a leading slash may be left out
- Commands support the placeholders ${player} and ${x} ${y} ${z}, and random tokens such as ${random:item}
- An entry whose command fails validation is never drawn, and the status line tells the reason
- The trigger message is a plain string that describes the reward only, because the chat line already reads You received s; never add such a prefix yourself, and a translation key or component JSON never appears here again
- An empty trigger message means no message at all for that entry

## Selecting and removing

- A plain click selects one row and clears the earlier selection
- Holding the control key adds a row to the selection or takes it out, and the row clicked last is the primary row
- The weight, the colour, the major event, the enabled state, the command content and the trigger message are written into every selected entry
- The entry name is written into the primary row only
- Removing takes two clicks: the first one only relabels the button, a second one inside the window really removes, and a timeout or a changed selection restores it

## Offering value configuration

- The columns are the name and the value
- The value tab opens this page on the first click, and every later click cycles the filter through offering values, item values and creature values without clearing the filter box; the box matches both names and registry names
- An entry that is not registered is drawn in a muted colour, and resting the pointer on it explains why
- The value field sits under the filter box, and the slider beneath it shows the same number: the slider drags in hundreds from 0 to 100000, while the field is for the exact number
- A filled value is written into every selected entry

## The admin list

- The admin button in the top right corner opens the list window, which holds the creator and every admin
- Only the creator may add or remove admins, and the other admins may only look
- The creator can never be removed
- Type a player name and press add: an empty name, a duplicate name and the creator himself are all refused
- A change of the list takes effect at once for an online player, and on the next join for an offline one

## Running a dealer wager

- One dealer hosts one wager at a time and recruits for one minute
- A wager with too few members or without the one who started it is cancelled with every stake and the dice returned
- A dealer killed while a wager runs stops that wager as well, returns every stake and the dice, and closes the dealer page a player still has open
- A fallen dealer drops the gambler dice still left in its pocket
- The first seconds of the recruit and its last seconds carry a chime, which helps read the pace

## Submitting and reloading

- Submit writes the changes to disk; on a revision conflict the panel loads the latest configuration, so another submit works
- A failed save reports its reason on the status line
- Run /devilbonegambler reload (or /dbgr reload) to make the server read the configuration again
- This manual follows the role: it is granted while the role lasts and taken back with it; a dropped copy disappears after a moment and is granted again; it goes back into the inventory when a container screen closes

## Good to know

- Every prompt appears in the chat box
- A command runs exactly as written, so try a change in a single player save first

## Mod command placeholder system

Placeholders go into the entry command and are replaced when the draw runs. The trigger message only supports ${player} and ${x} ${y} ${z}.

### Player and position

- ${player} is the name of the player who made the draw
- ${x} ${y} ${z} are that player's coordinates
- @(name) is the position of a named player; empty brackets mean the drawing player
- #(name) is the altar position of a named player, which is the player's feet while an altar is worn
- A position token may carry [random=mode&range] with a range of at most 64: mode 0 keeps the position, 1 spreads along the Z axis, 2 spreads over the horizontal plane and 3 spreads over the whole sphere; without it mode 0 and range 6 apply

### %(n,m) random integer

- Draws one integer between n and m, both inclusive; n must be smaller than m and both ends stay within 1000000
- On a dice draw the target rises with the point, the overflow and the luck; red leans high and black leans low
- A /dbgr run trial has no draw context and stays uniform inside the range
- Example: give ${player} minecraft:bread %(4,16)

### --R(n) repeat

- It only counts at the very end of a command line, trailing spaces ignored, and n is an integer from 1 to 10
- The line runs n times, and every other placeholder is expanded again for each repetition, so random integers and sample spaces may differ
- An entry with an invalid n or a suffix that is not at the end fails validation
- Example: give ${player} minecraft:bread 1 --R(5)

### Random tokens

- ${random:item}, ${random:effect}, ${random:entity}, ${random:enchantment} and ${random:potion} pick a random registry id of that kind
- The pools are rebuilt from the installed content on every world load and written to random_pools.json in the config directory

### Sample spaces

- ^(TYPE) or ^(TYPE)[namespace] picks one id from that type's sample space
- The types are discovered from the installed content after every world load and written to sample_spaces.json, so a game update or a new mod only changes that file
- Without the brackets the whole space is used; with them only that namespace is used
- A red point picks only positive samples and a black point only negative ones, falling back to the whole space when the type has none
- The higher the point and the overflow, the more the pick leans to the rare part of the space; a higher luck value also leans to rare and positive samples
- General types: ITEM, BLOCK, ENTITY, MOB, EFFECT, ENCHANTMENT, POTION, SOUND, PARTICLE, PAINTING, ATTRIBUTE, FLUID, BIOME, STRUCTURE, DAMAGE_TYPE
- Focused types: BUFF, DEBUFF and NEUBUFF pick effects by category, BUFF_ENCHANTMENT picks positive enchantments, CURSE picks curses, and ENCHANTED_BOOK hands out an enchanted book directly
- One line may carry several placeholders, each sampled on its own
- A placeholder only samples a registry id and never spawns an entity or places a block; the enchanted book is the one exception
- After the draw the sampled names are appended to the trigger message, separated by spaces; entries without placeholders are untouched

### Validation and trial runs

- An unknown type, an empty namespace filter, an empty range or an out of bounds end invalidates the entry
- Validation replaces every placeholder with a representative sample before the vanilla parser reads the line
- Try a line in chat with /dbgr run <command with placeholders>; only the creator and the admins may use it
- The sampled id is plain text inside the command, so the command still follows the vanilla syntax; try a changed line in a single player world first
