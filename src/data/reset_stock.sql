-- Puts every product back to 20 in stock and available (run on the existing database).
USE gudorestobardb;

UPDATE MENU_ITEM
SET stockQuantity = 10,
    availabilityStatus = 'Available';

SELECT itemID, itemName, stockQuantity, availabilityStatus FROM MENU_ITEM;
