-- Active: 1789518258812@@127.0.0.1@3306@gudorestobardb
-- SQLBook: Code
-- Active


-- Task 4: Create database
CREATE DATABASE IF NOT EXISTS gudorestobardb;

USE gudorestobardb;

-- only use for emergencies
-- DROP TABLE PAYMENT;
-- DROP TABLE ORDER_DETAIL;
-- DROP TABLE CUSTOMER_ORDER;
-- DROP TABLE MENU_ITEM;

CREATE TABLE MENU_ITEM (
  itemID INT PRIMARY KEY,
  itemName VARCHAR(200) NOT NULL,
  category VARCHAR(20) NOT NULL
    CHECK (category IN ('Meals', 'Snacks', 'Drinks', 'Extras')),
  unitPrice DECIMAL(10,2) NOT NULL,
  stockQuantity INT NOT NULL,
  availabilityStatus VARCHAR(20) DEFAULT 'Available'
    CHECK (availabilityStatus IN ('Available', 'Not Available'))
);

CREATE TABLE CUSTOMER_ORDER (
  orderID INT AUTO_INCREMENT PRIMARY KEY,
  orderDate DATE NOT NULL,
  orderTime TIME NOT NULL,
  orderType VARCHAR(20) NOT NULL
    CHECK (orderType IN ('Dine-in', 'Takeout')),
  totalAmount DECIMAL(10,2)
    CONSTRAINT chk_positive CHECK (totalAmount > 0.00),
  paymentStatus VARCHAR(20) DEFAULT 'Pending'
    CHECK (paymentStatus IN ('Pending', 'Paid', 'Cancelled')),
  orderStatus VARCHAR(20) DEFAULT 'Preparing'
    CHECK (orderStatus IN  ('Preparing', 'Served', 'Completed'))
);

CREATE TABLE ORDER_DETAIL (
  detailID INT AUTO_INCREMENT PRIMARY KEY,
  orderID INT NOT NULL,
  itemID INT NOT NULL,
  quantity INT NOT NULL
    CONSTRAINT chk_positive_quantity CHECK (quantity >= 1),
  price DECIMAL(10,2) NOT NULL,
  wantWater BOOLEAN NOT NULL DEFAULT FALSE,
  wantUtensils BOOLEAN NOT NULL DEFAULT FALSE,
  subTotal DECIMAL(10,2)
    GENERATED ALWAYS AS (quantity * price) STORED,

    CONSTRAINT chk_positive_subTotal CHECK (subTotal > 0.00),

    FOREIGN KEY (orderID) REFERENCES customer_order(orderID),
    FOREIGN KEY (itemID) REFERENCES menu_item(itemID)
);

CREATE TABLE PAYMENT (
  paymentID INT AUTO_INCREMENT PRIMARY KEY,
  orderID INT NOT NULL,
  method VARCHAR(20)
    CHECK (method IN ('Cash', 'Cashless')),
  transactionStatus VARCHAR(20) DEFAULT 'Pending'
    CHECK (transactionStatus IN ('Pending', 'Successful')),
  amount DECIMAL(10,2),
  paymentDate DATE,

    FOREIGN KEY (orderID) REFERENCES customer_order(orderID)
);

-- Task 5: Populate tables with sufficient records (ALL PREDEFINED RECORDS)
/*
MENU_ITEM primary key categorization
- 100+ --> meals
- 200+ --> snacks
- 300+ --> drinks
- 400+ --> extras
*/
INSERT INTO MENU_ITEM
  (itemID,  itemName,                             category,   unitPrice,  stockQuantity,  availabilityStatus)
  VALUES
  (101,     'Alejandro Balls',                    'Meals',    79.00,      20,             'Available'),
  (102,     'Peanut Butterbonia Pancit Canton',   'Meals',    109.00,     20,             'Available'),
  (103,     '.Swing Sisig',                       'Meals',    149.00,     20,             'Available'),
  (104,     'Quiburger',                          'Meals',    179.00,     20,             'Available'),
  (105,     'Object Oriented Pasta',              'Meals',    189.00,     20,             'Available'),
  (106,     'Lechong Gudo',                       'Meals',    299.00,     20,             'Available'),

  (201,     'LinkedHash Brown',                   'Snacks',   59.00,      20,             'Available'),
  (202,     '4-Pillar Fries',                     'Snacks',   110.00,     20,             'Available'),
  (203,     'Chili con Claude',                   'Snacks',   149.00,     20,             'Available'),
  (204,     'JNuggets',                           'Snacks',   189.00,     20,             'Available'),
  (205,     'Snhackathon Package',                'Snacks',   399.00,     20,             'Available'),

  (301,     "'Jandro Cola",                       'Drinks',   75.00,      20,             'Available'),
  (302,     'San Gudo Light',                     'Drinks',   89.00,      20,             'Available'),
  (303,     'San Gudo',                           'Drinks',   100.00,     20,             'Available'),
  (304,     'Sam Smoothie',                       'Drinks',   159.00,     20,             'Available'),
  (305,     'Java Milkshake',                     'Drinks',   199.99,     20,             'Available'),
  
  (401,     'BBQ Sauce',                          'Extras',   20.00,      20,             'Available'),
  (402,     'Cheese Sauce',                       'Extras',   20.00,      20,             'Available'),
  (403,     'Extra Rice',                         'Extras',   30.00,      20,             'Available'),
  (404,     'Java Rice',                          'Extras',   40.00,      20,             'Available');

INSERT INTO CUSTOMER_ORDER
    (orderDate,     orderTime,    orderType,    paymentStatus,  orderStatus)
VALUES
    ('2026-10-20',  '11:30:00',   'Dine-in',    'Paid',         'Completed'),
    ('2026-10-21',  '13:15:00',   'Takeout',    'Paid',         'Completed'),
    ('2026-10-22',  '18:45:00',   'Dine-in',    'Paid',         'Preparing'),
    ('2026-10-22',  '12:10:00',   'Takeout',    'Paid',         'Completed'),
    ('2026-10-23',  '09:45:00',   'Dine-in',    'Paid',         'Completed'),
    ('2026-10-23',  '14:20:00',   'Takeout',    'Paid',         'Preparing'),
    ('2026-10-24',  '11:05:00',   'Dine-in',    'Paid',         'Preparing');


INSERT INTO ORDER_DETAIL
    (orderID, itemID,   quantity, price,    wantWater,  wantUtensils)
VALUES
    (1,       101,      2,        79.00,    FALSE,      TRUE),
    (1,       301,      2,        75.00,    FALSE,      TRUE),

    (2,       104,      1,        179.00,   FALSE,      TRUE),
    (2,       201,      2,        59.00,    TRUE,       TRUE),

    (3,       106,      1,        299.00,   TRUE,       TRUE),
    (3,       304,      1,        159.00,   FALSE,      TRUE),

    (4,       102,      1,        109.00,   FALSE,      TRUE),
    (4,       202,      1,        110.00,   TRUE,       TRUE),
    (4,       301,      1,        75.00,    FALSE,      TRUE),

    (5,       103,      1,        149.00,   FALSE,      TRUE),
    (5,       201,      1,        59.00,    TRUE,       TRUE),

    (6,       105,      1,        189.00,   TRUE,       TRUE),
    (6,       203,      1,        149.00,   TRUE,       TRUE),
    (6,       301,      1,        75.00,    TRUE,       TRUE),

    (7,       104,      1,        179.00,   FALSE,      TRUE),
    (7,       201,      1,        59.00,    FALSE,      TRUE),
    (7,       301,      1,        75.00,    FALSE,      TRUE);


INSERT INTO PAYMENT
    (orderID,  method,       transactionStatus,   amount,       paymentDate)
VALUES
    (1,        'Cash',       'Successful',        308.00,       '2026-10-20'),
    (2,        'Cashless',   'Successful',        297.00,       '2026-10-21'),
    (3,        'Cash',       'Successful',        458.00,       '2026-10-22'),
    (4,        'Cash',       'Successful',        294.00,       '2026-10-22'),
    (5,        'Cashless',   'Successful',        208.00,       '2026-10-23'),
    (6,        'Cashless',   'Successful',        413.00,       '2026-10-23'),
    (7,        'Cash',       'Successful',        313.00,       '2026-10-24');


-- Instructor Checkpoint
SHOW TABLES;
SELECT * FROM MENU_ITEM;
SELECT * FROM CUSTOMER_ORDER;
SELECT * FROM ORDER_DETAIL;
SELECT * FROM PAYMENT;