output "alb_id" {
  value = aws_lb.this.id
}

output "alb_dns_name" {
  value = aws_lb.this.dns_name
}

output "target_group_arn" {
  description = "Product Service target group ARN"
  value       = aws_lb_target_group.app.arn
}

output "inventory_target_group_arn" {
  description = "Inventory Service target group ARN"
  value       = aws_lb_target_group.inventory.arn
}

output "payment_target_group_arn" {
  description = "Payment Service target group ARN"
  value       = aws_lb_target_group.payment.arn
}

output "order_target_group_arn" {
  description = "Order Service target group ARN"
  value       = aws_lb_target_group.order.arn
}

# =========================================================
# Shipping Service
# =========================================================

output "shipping_target_group_arn" {
  description = "Shipping Service ALB target group ARN"
  value       = aws_lb_target_group.shipping.arn
}


# =========================================================
# Notification Service
# =========================================================

output "notification_target_group_arn" {
  description = "Notification Service ALB target group ARN"
  value       = aws_lb_target_group.notification.arn
}


output "user_target_group_arn" {
  description = "User Service ALB target group ARN"
  value       = aws_lb_target_group.user.arn
}


output "grafana_target_group_arn" {
  description = "Grafana Service ALB target group ARN"
  value       = aws_lb_target_group.grafana.arn
}