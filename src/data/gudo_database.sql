-- SQLBook: Code
-- Active: 1790556296860@@127.0.0.1@3306@gudorestobardb
CREATE DATABASE IF NOT EXISTS gudorestobardb;

USE gudorestobardb;

-- only use for emergencies
DROP TABLE PAYMENT;
DROP TABLE ORDER_DETAIL;
DROP TABLE SALES_MANAGER;
DROP TABLE CUSTOMER_ORDER;
DROP TABLE MENU_ITEM;

CREATE TABLE MENU_ITEM (
  itemID INT PRIMARY KEY,
  itemName VARCHAR(200) NOT NULL,
  category VARCHAR(20) NOT NULL,
  unitPrice DECIMAL(10,2) NOT NULL,
  stockQuantity INT NOT NULL,
  availabilityStatus VARCHAR(20) DEFAULT 'Available'
);

CREATE TABLE CUSTOMER_ORDER (
  orderID INT AUTO_INCREMENT PRIMARY KEY,
  orderDate DATE,
  orderTime TIME,
  orderType VARCHAR(20),
  totalAmount DECIMAL(10,2),
  paymentStatus VARCHAR(20) DEFAULT 'Pending',
  orderStatus VARCHAR(20) DEFAULT 'Preparing'
);

CREATE TABLE ORDER_DETAIL (
  detailID INT AUTO_INCREMENT PRIMARY KEY,
  orderID INT NOT NULL,
  itemID INT NOT NULL,
  quantity INT,
  price DECIMAL(10,2),
  subTotal DECIMAL(10,2),

    FOREIGN KEY (orderID) REFERENCES customer_order(orderID),
    FOREIGN KEY (itemID) REFERENCES menu_item(itemID)
);

CREATE TABLE PAYMENT (
  paymentID INT PRIMARY KEY,
  orderID INT NOT NULL,
  method VARCHAR(20),
  status VARCHAR(20) DEFAULT 'Pending',
  amount INT,
  paymentDate DATE,

    FOREIGN KEY (orderID) REFERENCES customer_order(orderID)
);

SHOW TABLES;
/*
MENU_ITEM primary key chu4
- 100+ --> meals
- 200+ --> snacks
- 300+ --> drinks
*/
INSERT INTO MENU_ITEM
  (itemID,  itemName,                             category,   unitPrice,  stockQuantity,  availabilityStatus)
  VALUES
  (101,     'Alejandro Balls',                    'Meals',    79.00,      10,             'Available'),
  (102,     'Peanut Butterbonia Pancit Canton',   'Meals',    109.00,     10,             'Available'),
  (103,     '.Swing Sisig',                       'Meals',    149.00,     10,             'Available'),
  (104,     'Quiburger',                          'Meals',    179.00,     10,             'Available'),
  (105,     'Object Oriented Pasta',              'Meals',    189.00,     10,             'Available'),
  (106,     'Lechong Gudo',                       'Meals',    299.00,     10,             'Available'),

  (201,     'LinkedHash Brown',                   'Snacks',   59.00,      10,             'Available'),
  (202,     '4-Pillar Fries',                     'Snacks',   110.00,     10,             'Available'),
  (203,     'Chili con Claude',                   'Snacks',   149.00,     10,             'Available'),
  (204,     'JNuggets',                           'Snacks',   189.00,     10,             'Available'),
  (205,     'Snhackathon Package',                'Snacks',   399.00,     10,             'Available'),

  (301,     "'Jandro Cola",                       'Drinks',   75.00,      10,             'Available'),
  (302,     'San Gudo Light',                     'Drinks',   89.00,      10,             'Available'),
  (303,     'San Gudo',                           'Drinks',   100.00,     10,             'Available'),
  (304,     'Sam Smoothie',                       'Drinks',   159.00,     10,             'Available'),
  (305,     'Java Milkshake',                     'Drinks',   199.99,     10,             'Available');

INSERT INTO CUSTOMER_ORDER
    (orderDate,     orderTime,    orderType,  totalAmount,  paymentStatus,  orderStatus)
VALUES
    ('2026-10-20',  '11:30:00',   'Dine-in',  308.00,       'Paid',         'Completed'),
    ('2026-10-21',  '13:15:00',   'Takeout',  297.00,       'Paid',         'Completed'),
    ('2026-10-22',  '18:45:00',   'Dine-in',  458.00,       'Paid',         'Preparing'),
    ('2026-10-22',  '12:10:00',   'Takeout',  268.00,       'Paid',         'Completed'),
    ('2026-10-23',  '09:45:00',   'Dine-in',  184.00,       'Paid',         'Completed'),
    ('2026-10-23',  '14:20:00',   'Takeout',  348.00,       'Paid',         'Preparing'),
    ('2026-10-24',  '11:05:00',   'Dine-in',  259.00,       'Paid',         'Preparing');


INSERT INTO ORDER_DETAIL
    (orderID, itemID,   quantity, price,    subTotal)
VALUES
    (1,       101,      2,        79.00,    158.00),
    (1,       301,      2,        75.00,    150.00),

    (2,       104,      1,        179.00,   179.00),
    (2,       201,      2,        59.00,    118.00),

    (3,       106,      1,        299.00,   299.00),
    (3,       304,      1,        159.00,   159.00),

    (4,       102,      1,        109.00,   109.00),
    (4,       202,      1,        110.00,   110.00),
    (4,       301,      1,        75.00,    75.00),

    (5,       103,      1,        149.00,   149.00),
    (5,       201,      1,        59.00,    59.00),

    (6,       105,      1,        189.00,   189.00),
    (6,       203,      1,        149.00,   149.00),
    (6,       301,      1,        75.00,    75.00),

    (7,       104,      1,        179.00,   179.00),
    (7,       201,      1,        59.00,    59.00),
    (7,       301,      1,        75.00,    75.00);


INSERT INTO PAYMENT
    (paymentID, orderID,  method,       status,             amount,       paymentDate)
VALUES
    (1,         1,        'Cash',       'Completed',        308.00,       '2026-10-20'),
    (2,         2,        'Cash',       'Completed',        297.00,       '2026-10-21'),
    (3,         3,        'Cash',       'Completed',        458.00,       '2026-10-22'),
    (4,         4,        'Cash',       'Completed',        294.00,       '2026-10-22'),
    (5,         5,        'Cash',       'Completed',        208.00,       '2026-10-23'),
    (6,         6,        'Cash',       'Completed',        413.00,       '2026-10-23'),
    (7,         7,        'Cash',       'Completed',        313.00,       '2026-10-24');;


