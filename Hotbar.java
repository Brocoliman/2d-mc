public class Hotbar {
    public String[] items;
    public int[] amounts;
    LoadExtData ext_data;

    Hotbar(LoadExtData ext_data) {
        this.items = new String[9];
        this.amounts = new int[9];
        this.ext_data = ext_data;
    }

    Hotbar(String[] item, int[] amount, LoadExtData ext_data) {
        this.items = item;
        this.amounts = amount;
        this.ext_data = ext_data;
    }

    public void insert(String item, int amount) { // also cleans up the hotbar
        for (int i = 0; i < 9; i++) {
            // Clear empty slots
            if (this.amounts[i] == 0) {
                this.items[i] = "";
            }

            // Check if slot is available
            if (this.items[i].equals(item)) {
                int slot_amount_add;
                // Insert until full or insert remaining amount left to insert
                slot_amount_add = Math.min(
                        ext_data.stack_size_data.get(this.items[i].split("/")[1])-this.amounts[i], // only retrieve item name
                        amount);
                amount -= slot_amount_add;
                this.amounts[i] += slot_amount_add;
            }
            // If the stack is fully inserted, break
            if (amount == 0) break;
        }
        // If no slot with same item, insert into extra slot
        if (amount > 0) {
            for (int i = 0; i < 9; i++) {
                // Find empty slot
                if (this.items[i].equals("")) {
                    this.items[i] = item;
                    this.amounts[i] = amount;
                    break;
                }
            }
        }
    }
}
