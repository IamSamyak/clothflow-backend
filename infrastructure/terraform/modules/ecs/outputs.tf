output "cluster_id" {
  description = "ECS cluster ID"
  value       = aws_ecs_cluster.this.id
}

output "cluster_name" {
  description = "ECS cluster name"
  value       = aws_ecs_cluster.this.name
}


# =========================================================
# Product Service
# =========================================================

output "product_service_task_definition_arn" {
  description = "Product Service ECS task definition ARN"
  value       = aws_ecs_task_definition.product_service.arn
}

output "product_service_id" {
  description = "ECS Product Service ID"
  value       = aws_ecs_service.product_service.id
}

output "product_service_name" {
  description = "ECS Product Service name"
  value       = aws_ecs_service.product_service.name
}


# =========================================================
# Inventory Service
# =========================================================

output "inventory_service_task_definition_arn" {
  description = "Inventory Service ECS task definition ARN"
  value       = aws_ecs_task_definition.inventory_service.arn
}

output "inventory_service_id" {
  description = "ECS Inventory Service ID"
  value       = aws_ecs_service.inventory_service.id
}

output "inventory_service_name" {
  description = "ECS Inventory Service name"
  value       = aws_ecs_service.inventory_service.name
}

# =========================================================
# Payment Service
# =========================================================


output "payment_service_task_definition_arn" {
  description = "Payment Service ECS task definition ARN"
  value       = aws_ecs_task_definition.payment_service.arn
}

output "payment_service_id" {
  description = "ECS Payment Service ID"
  value       = aws_ecs_service.payment_service.id
}

output "payment_service_name" {
  description = "ECS Payment Service name"
  value       = aws_ecs_service.payment_service.name
}


# =========================================================
# Order Service
# =========================================================

output "order_service_task_definition_arn" {
  description = "Order Service ECS task definition ARN"
  value       = aws_ecs_task_definition.order_service.arn
}

output "order_service_id" {
  description = "ECS Order Service ID"
  value       = aws_ecs_service.order_service.id
}

output "order_service_name" {
  description = "ECS Order Service name"
  value       = aws_ecs_service.order_service.name
}