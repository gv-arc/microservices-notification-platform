-- Initialize databases and users for local development
CREATE DATABASE IF NOT EXISTS user_db;
CREATE DATABASE IF NOT EXISTS notification_db;

CREATE USER IF NOT EXISTS 'user_app'@'%' IDENTIFIED BY 'user_secret';
CREATE USER IF NOT EXISTS 'notification_app'@'%' IDENTIFIED BY 'notification_secret';

GRANT ALL PRIVILEGES ON user_db.* TO 'user_app'@'%';
GRANT ALL PRIVILEGES ON notification_db.* TO 'notification_app'@'%';

FLUSH PRIVILEGES;
