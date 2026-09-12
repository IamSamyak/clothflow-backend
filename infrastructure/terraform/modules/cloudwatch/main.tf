resource "aws_cloudwatch_log_group" "product_service" {
  name = "/ecs/clothflow/product-service"

  retention_in_days = 14

  tags = {
    Project     = "clothflow"
    Environment = var.environment
    Service     = "product-service"
  }
}

resource "aws_cloudwatch_log_group" "inventory_service" {
  name = "/ecs/clothflow/inventory-service"

  retention_in_days = 14

  tags = {
    Project     = "clothflow"
    Environment = var.environment
    Service     = "inventory-service"
  }
}

resource "aws_cloudwatch_log_group" "payment_service" {
  name = "/ecs/clothflow/payment-service"

  retention_in_days = 14

  tags = {
    Project     = "clothflow"
    Environment = var.environment
    Service     = "payment-service"
  }
}

resource "aws_cloudwatch_log_group" "order_service" {
  name = "/ecs/clothflow/order-service"

  retention_in_days = 14

  tags = {
    Project     = "clothflow"
    Environment = var.environment
    Service     = "order-service"
  }
}

# =========================================================
# Shipping Service
# =========================================================

resource "aws_cloudwatch_log_group" "shipping_service" {
  name              = "/ecs/clothflow/shipping-service"
  retention_in_days = 14

  tags = {
    Project     = "clothflow"
    Environment = var.environment
    Service     = "shipping-service"
  }
}


# =========================================================
# Notification Service
# =========================================================

resource "aws_cloudwatch_log_group" "notification_service" {
  name              = "/ecs/clothflow/notification-service"
  retention_in_days = 14

  tags = {
    Project     = "clothflow"
    Environment = var.environment
    Service     = "notification-service"
  }
}


# =========================================================
# User Service
# =========================================================

resource "aws_cloudwatch_log_group" "user_service" {
  name              = "/ecs/clothflow/user-service"
  retention_in_days = 14

  tags = {
    Project     = "clothflow"
    Environment = var.environment
    Service     = "user-service"
  }
}


resource "aws_cloudwatch_log_group" "prometheus" {
  name              = "/ecs/clothflow/prometheus"
  retention_in_days = 14

  tags = {
    Project     = "clothflow"
    Environment = "dev"
    Service     = "prometheus"
  }
}

resource "aws_cloudwatch_log_group" "grafana" {
  name              = "/ecs/clothflow/grafana"
  retention_in_days = 14

  tags = {
    Project     = "clothflow"
    Environment = "dev"
    Service     = "grafana"
  }
}