# Creator's Manual

## Who you are

- The first player to join the world is recorded as the creator when the save is loaded for the first time
- The creator is recorded once and never changes afterwards, and nobody can remove the creator
- The creator holds every admin ability and one more, the admin list

## What only you can do

- Add and remove admins in the admin window of the panel; the prize and the offering value configuration are yours to change as well

## How a dealer appears

- It is only attempted when the world time reaches the sunrise or the sunset tick, and one dealer is created per such tick at most
- A candidate must hold the Gambler's Dice in the main hand or the off hand, and a player without one is never picked
- The candidates are shuffled and tried in turn: a player whose chunk already holds a dealer is skipped, and the first usable position creates a dealer near that player
- This rule is written here only

## How the manuals are handed out and taken back

- Every join compares the inventory against the role, so what is deserved is granted and what is no longer deserved is taken back
- This creator's manual and the admin manual need no care: a dropped copy disappears after a moment and is granted again

---

# Admin Manual

## Opening the panel

- Press Shift and D in game, and the key can be rebound in the vanilla controls screen
- Or run /devilbonegambler config, the short form /dbgr config is equivalent
- Only the creator and the admins may open it, a press by anybody else does nothing

## Panel overview

- The tabs on the top are the prize configuration and the offering values
- The upper part edits the fields and the lower part is the table: the header stays frozen, the columns stay fixed, long content is clipped, and resting the pointer on it shows it in full
- The scroll bar can be dragged, and the status line reports the last action

## Prize configuration

- The columns are the region, the entry name, the weight and the colour
- Adding a region appends one, and the region number matches a dice point; removing a selected region removes its entries as well
- Adding an entry appends one row in the current region, with the fields region number, entry name, weight, colour, major event, enabled, command text and display text
- The entry name is only a note, and a preset entry follows the language of the game
- A higher weight is drawn more often; red is positive and black is negative
- The colour, the major event and the enabled state are buttons, one click toggles one of them
- The command text may hold several lines, one vanilla command per line, and a leading slash may be left out
- Commands support the placeholders ${player} and ${x} ${y} ${z}, and random tokens such as ${random:item}
- An entry whose command fails validation is never drawn, and the status line tells the reason
- An empty display text falls back to the default wording

## Selecting and removing

- A plain click selects one row and clears the earlier selection
- Holding the control key adds a row to the selection or takes it out, and the row clicked last is the primary row
- The weight, the colour, the major event, the enabled state, the command text and the display text are written into every selected entry
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
