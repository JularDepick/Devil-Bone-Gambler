# Creator's Manual

## Who the creator is

- While the world loads, the first player to join is recorded as the creator whenever the management access file has no creator yet
- The creator is recorded once and never changes afterwards
- The creator can never be removed

## Creator only abilities

- Open the admin panel: Shift and D, or the command /devilbonegambler config
- Add and remove admins
- Change the prize pool configuration and the offering value configuration

## Configuration files of the save

- Prize pool: <world save>/serverconfig/devil_bone_gambler/prize_pool.json
- Offering values: <world save>/serverconfig/devil_bone_gambler/item_values.json
- Random pools: <world save>/serverconfig/devil_bone_gambler/random_pools.json
- Management access: <world save>/serverconfig/devil_bone_gambler/access.json
- Back up those files before editing them, access.json and prize_pool.json above all

## How the manuals are handed out

- Gameplay Manual: handed to every player on the first join
- Admin Manual: handed to a granted admin that is not the creator on the first join
- Creator's Manual: handed to the creator on the first join
- Each manual is handed out once per player, and none of them is craftable or stackable

## Admin manual content

The content below is identical to the Admin Manual.

---

# Admin Manual

## Opening the panel

- Press Shift and D in game, and the key can be rebound in the vanilla controls screen
- Or run the command /devilbonegambler config
- Only the creator and the granted admins may open the panel

## Prize pool configuration

- The list is a table: the header is frozen, the columns are fixed, long content is clipped, and resting the pointer shows the full content
- Region: add one with the add region button, and the region number matches a dice point
- Entry: one entry per row, with the fields region number, entry name, weight, colour, major event, enabled, command and display text
- The entry name is only a note that tells entries apart; preset entries follow the language of the game
- A higher weight is drawn more often; a red entry is positive and a black entry is negative
- A command may hold several lines, one vanilla command per line, and a leading slash may be left out
- Commands support the placeholders ${player} and ${x} ${y} ${z}, and the random tokens such as ${random:item}
- An entry whose command fails validation is never drawn, and the status line tells the reason
- An empty display text falls back to the default wording

## Multiple selection and removal

- A plain click selects one row and clears the earlier selection
- Holding the control key adds rows to the selection, and the row clicked last is the primary row
- Changing the weight, colour, major event, enabled state, command or display text writes the value into every selected entry
- The entry name is written into the primary row only
- The remove button deletes every selected row, and its first click only relabels the button

## Offering value configuration

- Two columns: name and value
- The top button cycles the chapters all, offering values and creature values
- The filter box matches both the name and the registry name
- An entry that is not registered is drawn in a muted colour, and a hover explains why
- A number is written into every selected entry

## Admin list

- Only the creator may add or remove admins
- The creator can never be removed
- Type a player name and press add; an empty name, a duplicate name and the creator himself are all refused

## Submitting and saving

- Press submit to write the files
- On a revision conflict the panel loads the latest configuration, and submitting again works
- A failed save reports its reason on the status line

## Prompt channel

- Every prompt of the mod appears in the chat box only, the area above the hotbar stays free
