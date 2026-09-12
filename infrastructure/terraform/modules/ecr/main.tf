resource "aws_ecr_repository" "product_service" {
  name                 = "clothflow/product-service"
  image_tag_mutability = "IMMUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name        = "clothflow-product-service"
    Project     = "clothflow"
    Environment = "dev"
    Service     = "product-service"
  }
}

resource "aws_ecr_repository" "inventory_service" {
  name                 = "clothflow/inventory-service"
  image_tag_mutability = "IMMUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name        = "clothflow-inventory-service"
    Project     = "clothflow"
    Environment = "dev"
    Service     = "inventory-service"
  }
}

# =========================================================
# Payment Service
# =========================================================

resource "aws_ecr_repository" "payment_service" {

  name                 = "clothflow/payment-service"
  image_tag_mutability = "IMMUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name        = "clothflow-payment-service"
    Project     = "clothflow"
    Environment = "dev"
    Service     = "payment-service"
  }
}

# =========================================================
# Order Service
# =========================================================

resource "aws_ecr_repository" "order_service" {
  name                 = "clothflow/order-service"
  image_tag_mutability = "IMMUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name        = "clothflow-order-service"
    Project     = "clothflow"
    Environment = "dev"
    Service     = "order-service"
  }
}

# =========================================================
# Shipping Service
# =========================================================

resource "aws_ecr_repository" "shipping_service" {
  name                 = "clothflow/shipping-service"
  image_tag_mutability = "IMMUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name        = "clothflow-shipping-service"
    Project     = "clothflow"
    Environment = "dev"
    Service     = "shipping-service"
  }
}


# =========================================================
# Notification Service
# =========================================================

resource "aws_ecr_repository" "notification_service" {
  name                 = "clothflow/notification-service"
  image_tag_mutability = "IMMUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name        = "clothflow-notification-service"
    Project     = "clothflow"
    Environment = "dev"
    Service     = "notification-service"
  }
}

resource "aws_ecr_repository" "user_service" {
  name = "clothflow/user-service"

  image_tag_mutability = "IMMUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Project     = "clothflow"
    Environment = "dev"
    Service     = "user-service"
  }
}

# =========================================================
# Prometheus
# =========================================================

resource "aws_ecr_repository" "prometheus" {
  name                 = "clothflow/prometheus"
  image_tag_mutability = "IMMUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name        = "clothflow-prometheus"
    Project     = "clothflow"
    Environment = "dev"
    Service     = "prometheus"
  }
}

resource "aws_ecr_repository" "grafana" {
  name                 = "clothflow/grafana"
  image_tag_mutability = "IMMUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name        = "clothflow-grafana"
    Project     = "clothflow"
    Environment = "dev"
    Service     = "grafana"
  }
}