variable "product_container_image" {
  description = "Product Service container image"
  type        = string
}

variable "inventory_container_image" {
  description = "Inventory Service container image"
  type        = string
}

variable "payment_container_image" {
  description = "Container image for Payment Service"
  type        = string
}

variable "order_container_image" {
  description = "Container image for Order Service"
  type        = string
}

variable "shipping_container_image" {
  description = "Docker image for Shipping Service"
  type        = string
}

variable "notification_container_image" {
  description = "Docker image for Notification Service"
  type        = string
}

variable "user_container_image" {
  description = "Container image for User Service"
  type        = string
}
