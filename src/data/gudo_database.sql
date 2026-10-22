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

CREATE TABLE SALES_MANAGER (
  managerID INT PRIMARY KEY,
  lastName VARCHAR(30) UNIQUE,
  firstName VARCHAR(30),
  email VARCHAR(100) UNIQUE,
  contactNumber VARCHAR(15) UNIQUE
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
  managerID INT NOT NULL,
  method VARCHAR(20),
  status VARCHAR(20) DEFAULT 'Pending',
  amount INT,
  paymentDate DATE,

    FOREIGN KEY (orderID) REFERENCES customer_order(orderID),
    FOREIGN KEY (managerID) REFERENCES  sales_manager(managerID)
);

SHOW TABLES;
SELECT * FROM MENU_ITEM;
SELECT * FROM SALES_MANAGER;
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

INSERT INTO SALES_MANAGER
(managerID, lastName,     firstName,    email,                    contactNumber)
VALUES
(100,       'Samantha',   'Alejandro',  'alejantha@gmail.com',    '09561234567');


