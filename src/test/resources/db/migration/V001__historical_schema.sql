-- Historical DDL only. Execute through the guarded db-setup profile.
-- No historical records; no business schema corrections.

CREATE TABLE `sobol_dev`.`branch` (
  `branch_id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(150) NOT NULL,
  `city` varchar(100) NOT NULL,
  `phone` varchar(30) DEFAULT NULL,
  `warehouses_count` int NOT NULL DEFAULT '0',
  `total_pallet_capacity` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`branch_id`),
  UNIQUE KEY `name` (`name`),
  UNIQUE KEY `uq_branch_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `sobol_dev`.`category` (
  `category_id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(150) NOT NULL,
  `description` text,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`category_id`),
  UNIQUE KEY `unique_category_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `sobol_dev`.`customer` (
  `customer_id` int NOT NULL AUTO_INCREMENT,
  `full_name` varchar(150) NOT NULL,
  `phone` varchar(30) NOT NULL,
  `email` varchar(180) DEFAULT NULL,
  `city` varchar(100) DEFAULT NULL,
  `address` varchar(220) DEFAULT NULL,
  `notes` varchar(400) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`customer_id`),
  UNIQUE KEY `uq_customer_name` (`full_name`),
  UNIQUE KEY `uq_customer_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `sobol_dev`.`supplier` (
  `supplier_id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(150) NOT NULL,
  `phone` varchar(30) NOT NULL,
  `email` varchar(150) DEFAULT NULL,
  `city` varchar(100) DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `contact_person` varchar(150) DEFAULT NULL,
  `notes` varchar(400) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`supplier_id`),
  UNIQUE KEY `uq_supplier_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `sobol_dev`.`user_account` (
  `user_id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(160) NOT NULL,
  `email` varchar(160) NOT NULL,
  `phone` varchar(60) DEFAULT NULL,
  `password_hash` varchar(100) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `sobol_dev`.`user_database` (
  `user_id` int NOT NULL,
  `db_name` varchar(128) NOT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `db_name` (`db_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `sobol_dev`.`vehicle` (
  `vehicle_id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(150) NOT NULL,
  `plate_number` varchar(30) NOT NULL,
  `vin` varchar(50) NOT NULL,
  `vehicle_type` varchar(30) NOT NULL,
  `manufacturer` varchar(80) NOT NULL,
  `model` varchar(80) NOT NULL,
  `model_year` int NOT NULL,
  `status` varchar(30) NOT NULL,
  `capacity_pallets` int NOT NULL,
  `max_load_kg` int NOT NULL,
  `fuel_type` varchar(30) NOT NULL,
  `transmission` varchar(30) NOT NULL,
  `purchase_date` date NOT NULL,
  `last_service_date` date NOT NULL,
  `insurance_expiry` date NOT NULL,
  `image_path` varchar(500) DEFAULT NULL,
  `notes` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`vehicle_id`),
  UNIQUE KEY `uq_vehicle_name` (`name`),
  UNIQUE KEY `uq_vehicle_plate` (`plate_number`),
  UNIQUE KEY `uq_vehicle_vin` (`vin`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `sobol_dev`.`warehouse` (
  `warehouse_id` int NOT NULL AUTO_INCREMENT,
  `branch_id` int NOT NULL,
  `name` varchar(100) NOT NULL,
  `pallet_capacity` int NOT NULL,
  PRIMARY KEY (`warehouse_id`),
  KEY `fk_warehouse_branch` (`branch_id`),
  CONSTRAINT `fk_warehouse_branch` FOREIGN KEY (`branch_id`) REFERENCES `sobol_dev`.`branch` (`branch_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `sobol_dev`.`employee` (
  `employee_id` int NOT NULL AUTO_INCREMENT,
  `full_name` varchar(150) NOT NULL,
  `phone` varchar(30) NOT NULL,
  `branch_id` int NOT NULL,
  `hire_date` date NOT NULL,
  `employee_type` varchar(30) NOT NULL,
  `image_path` varchar(500) DEFAULT NULL,
  `worker_mode` varchar(20) DEFAULT NULL,
  `daily_rate` decimal(10,2) DEFAULT NULL,
  `hourly_rate` decimal(10,2) DEFAULT NULL,
  `driver_daily_rate` decimal(10,2) DEFAULT NULL,
  `vehicle_id` int DEFAULT NULL,
  `admin_daily_rate` decimal(10,2) DEFAULT NULL,
  `managed_branch_id` int DEFAULT NULL,
  `sales_daily_rate` decimal(10,2) DEFAULT NULL,
  `commission_rate` decimal(5,2) DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`employee_id`),
  UNIQUE KEY `uq_employee_fullname` (`full_name`),
  UNIQUE KEY `uq_employee_vehicle` (`vehicle_id`),
  KEY `fk_employee_branch` (`branch_id`),
  CONSTRAINT `fk_employee_branch` FOREIGN KEY (`branch_id`) REFERENCES `sobol_dev`.`branch` (`branch_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_employee_vehicle` FOREIGN KEY (`vehicle_id`) REFERENCES `sobol_dev`.`vehicle` (`vehicle_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `sobol_dev`.`product` (
  `product_id` int NOT NULL AUTO_INCREMENT,
  `category_id` int NOT NULL,
  `branch_id` int NOT NULL,
  `warehouse_id` int NOT NULL,
  `name` varchar(150) NOT NULL,
  `description` text,
  `base_unit` varchar(50) DEFAULT NULL,
  `cartons_per_pallet` int NOT NULL DEFAULT '0',
  `quantity` int NOT NULL DEFAULT '0',
  `min_quantity` int NOT NULL DEFAULT '0',
  `max_quantity` int NOT NULL DEFAULT '0',
  `cost_price` decimal(10,2) NOT NULL DEFAULT '0.00',
  `selling_price` decimal(10,2) NOT NULL DEFAULT '0.00',
  `image_path` varchar(500) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`product_id`),
  UNIQUE KEY `uq_product_name_per_category` (`category_id`,`name`),
  KEY `fk_product_branch` (`branch_id`),
  KEY `fk_product_warehouse` (`warehouse_id`),
  CONSTRAINT `fk_product_branch` FOREIGN KEY (`branch_id`) REFERENCES `sobol_dev`.`branch` (`branch_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_product_category` FOREIGN KEY (`category_id`) REFERENCES `sobol_dev`.`category` (`category_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_product_warehouse` FOREIGN KEY (`warehouse_id`) REFERENCES `sobol_dev`.`warehouse` (`warehouse_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `sobol_dev`.`attendance` (
  `attendance_id` int NOT NULL AUTO_INCREMENT,
  `employee_id` int NOT NULL,
  `work_date` date NOT NULL,
  `present` tinyint NOT NULL DEFAULT '1',
  `hours_worked` int DEFAULT NULL,
  `notes` varchar(300) DEFAULT NULL,
  PRIMARY KEY (`attendance_id`),
  UNIQUE KEY `uq_attendance` (`employee_id`,`work_date`),
  CONSTRAINT `fk_att_employee` FOREIGN KEY (`employee_id`) REFERENCES `sobol_dev`.`employee` (`employee_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `sobol_dev`.`import_shipment` (
  `shipment_id` int NOT NULL AUTO_INCREMENT,
  `product_id` int NOT NULL,
  `supplier_id` int NOT NULL,
  `branch_id` int NOT NULL,
  `warehouse_id` int NOT NULL,
  `quantity` int NOT NULL,
  `unit_cost` decimal(10,2) NOT NULL DEFAULT '0.00',
  `total_cost` decimal(12,2) NOT NULL DEFAULT '0.00',
  `shipment_date` date NOT NULL,
  `payment_type` varchar(20) NOT NULL DEFAULT 'CASH',
  `shipping_method` varchar(30) NOT NULL DEFAULT 'TRUCK',
  `reference_no` varchar(80) DEFAULT NULL,
  `notes` varchar(400) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`shipment_id`),
  KEY `fk_shipment_branch` (`branch_id`),
  KEY `idx_shipment_date` (`shipment_date`),
  KEY `idx_shipment_product` (`product_id`),
  KEY `idx_shipment_supplier` (`supplier_id`),
  KEY `idx_shipment_wh` (`warehouse_id`),
  CONSTRAINT `fk_shipment_branch` FOREIGN KEY (`branch_id`) REFERENCES `sobol_dev`.`branch` (`branch_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_shipment_product` FOREIGN KEY (`product_id`) REFERENCES `sobol_dev`.`product` (`product_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_shipment_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `sobol_dev`.`supplier` (`supplier_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_shipment_warehouse` FOREIGN KEY (`warehouse_id`) REFERENCES `sobol_dev`.`warehouse` (`warehouse_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `sobol_dev`.`sales_order` (
  `order_id` int NOT NULL AUTO_INCREMENT,
  `customer_id` int NOT NULL,
  `employee_id` int NOT NULL,
  `order_date` date NOT NULL,
  `total_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`order_id`),
  KEY `idx_sales_customer` (`customer_id`),
  KEY `idx_sales_employee` (`employee_id`),
  KEY `idx_sales_date` (`order_date`),
  CONSTRAINT `fk_sales_customer` FOREIGN KEY (`customer_id`) REFERENCES `sobol_dev`.`customer` (`customer_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_sales_employee` FOREIGN KEY (`employee_id`) REFERENCES `sobol_dev`.`employee` (`employee_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `sobol_dev`.`sales_order_item` (
  `item_id` int NOT NULL AUTO_INCREMENT,
  `order_id` int NOT NULL,
  `product_id` int NOT NULL,
  `quantity` int NOT NULL,
  `unit_price` decimal(10,2) NOT NULL DEFAULT '0.00',
  `line_total` decimal(12,2) NOT NULL DEFAULT '0.00',
  PRIMARY KEY (`item_id`),
  KEY `idx_item_order` (`order_id`),
  KEY `idx_item_product` (`product_id`),
  CONSTRAINT `fk_item_order` FOREIGN KEY (`order_id`) REFERENCES `sobol_dev`.`sales_order` (`order_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_item_product` FOREIGN KEY (`product_id`) REFERENCES `sobol_dev`.`product` (`product_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
