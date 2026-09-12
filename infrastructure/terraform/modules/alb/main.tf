resource "aws_lb" "this" {
  name               = "clothflow-alb"
  internal           = false
  load_balancer_type = "application"

  security_groups = [
    var.security_group_id
  ]

  subnets = var.public_subnet_ids

  enable_deletion_protection = false

  tags = {
    Name    = "clothflow-alb"
    Project = "clothflow"
  }
}


# =========================================================
# Product Service Target Group
# =========================================================

resource "aws_lb_target_group" "app" {
  name        = "clothflow-app-tg"
  port        = 8080
  protocol    = "HTTP"
  target_type = "ip"

  vpc_id = var.vpc_id

  health_check {
    enabled = true

    path = "/actuator/health"

    protocol = "HTTP"

    matcher = "200"

    interval = 30
    timeout  = 5

    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  tags = {
    Name    = "clothflow-app-tg"
    Project = "clothflow"
    Service = "product-service"
  }
}


# =========================================================
# Inventory Service Target Group
# =========================================================

resource "aws_lb_target_group" "inventory" {
  name        = "clothflow-inventory-tg"
  port        = 8082
  protocol    = "HTTP"
  target_type = "ip"

  vpc_id = var.vpc_id

  health_check {
    enabled = true

    path = "/actuator/health"

    protocol = "HTTP"

    matcher = "200"

    interval = 30
    timeout  = 5

    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  tags = {
    Name    = "clothflow-inventory-tg"
    Project = "clothflow"
    Service = "inventory-service"
  }
}


# =========================================================
# HTTP Listener
# =========================================================

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.this.arn

  port     = 80
  protocol = "HTTP"

  # Existing Product Service remains the default.
  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.app.arn
  }
}


# =========================================================
# Inventory Routing Rule
# =========================================================

resource "aws_lb_listener_rule" "inventory" {
  listener_arn = aws_lb_listener.http.arn

  priority = 100

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.inventory.arn
  }

  condition {
    path_pattern {
      values = [
        "/api/v1/inventory",
        "/api/v1/inventory/*"
      ]
    }
  }
}

# =========================================================
# Payment Service Target Group
# =========================================================

resource "aws_lb_target_group" "payment" {
  name        = "clothflow-payment-tg"
  port        = 8084
  protocol    = "HTTP"
  target_type = "ip"

  vpc_id = var.vpc_id

  health_check {
    enabled = true

    path = "/actuator/health"

    protocol = "HTTP"

    matcher = "200"

    interval = 30
    timeout  = 5

    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  tags = {
    Name    = "clothflow-payment-tg"
    Project = "clothflow"
    Service = "payment-service"
  }
}


# =========================================================
# Payment Routing Rule
# =========================================================

resource "aws_lb_listener_rule" "payment" {
  listener_arn = aws_lb_listener.http.arn

  priority = 200

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.payment.arn
  }

  condition {
    path_pattern {
      values = [
        "/api/v1/payments",
        "/api/v1/payments/*"
      ]
    }
  }
}

# =========================================================
# Order Service Target Group
# =========================================================

resource "aws_lb_target_group" "order" {
  name        = "clothflow-order-tg"
  port        = 8083
  protocol    = "HTTP"
  target_type = "ip"

  vpc_id = var.vpc_id

  health_check {
    enabled = true

    path = "/actuator/health"

    protocol = "HTTP"

    matcher = "200"

    interval = 30
    timeout  = 5

    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  tags = {
    Name    = "clothflow-order-tg"
    Project = "clothflow"
    Service = "order-service"
  }
}

# =========================================================
# Order Routing Rule
# =========================================================

resource "aws_lb_listener_rule" "order" {
  listener_arn = aws_lb_listener.http.arn

  priority = 300

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.order.arn
  }

  condition {
    path_pattern {
      values = [
        "/api/v1/orders",
        "/api/v1/orders/*"
      ]
    }
  }
}

# =========================================================
# Shipping Service
# =========================================================

resource "aws_lb_target_group" "shipping" {
  name        = "clothflow-shipping-tg"
  port        = 8085
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = var.vpc_id

  health_check {
    path                = "/actuator/health"
    protocol            = "HTTP"
    matcher             = "200"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  tags = {
    Project     = "clothflow"
    Service     = "shipping-service"
    Environment = "dev"
  }
}


# =========================================================
# Notification Service
# =========================================================

resource "aws_lb_target_group" "notification" {
  name        = "clothflow-notification-tg"
  port        = 8086
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = var.vpc_id

  health_check {
    path                = "/actuator/health"
    protocol            = "HTTP"
    matcher             = "200"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  tags = {
    Project     = "clothflow"
    Service     = "notification-service"
    Environment = "dev"
  }
}

# =========================================================
# User Service Target Group
# =========================================================

resource "aws_lb_target_group" "user" {
  name        = "clothflow-user-tg"
  port        = 8081
  protocol    = "HTTP"
  target_type = "ip"

  vpc_id = var.vpc_id

  health_check {
    enabled = true

    path = "/actuator/health"

    protocol = "HTTP"

    matcher = "200"

    interval = 30
    timeout  = 5

    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  tags = {
    Name        = "clothflow-user-tg"
    Project     = "clothflow"
    Service     = "user-service"
    Environment = "dev"
  }
}

# =========================================================
# User Service Routing Rule
# =========================================================

resource "aws_lb_listener_rule" "user" {
  listener_arn = aws_lb_listener.http.arn

  priority = 50

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.user.arn
  }

  condition {
    path_pattern {
      values = [
        "/api/v1/auth",
        "/api/v1/auth/*",
        "/.well-known/jwks.json"
      ]
    }
  }
}

# =========================================================
# Grafana Target Group
# =========================================================

resource "aws_lb_target_group" "grafana" {
  name        = "clothflow-grafana-tg"
  port        = 3000
  protocol    = "HTTP"
  target_type = "ip"

  vpc_id = var.vpc_id

  health_check {
    enabled = true

    path = "/api/health"

    protocol = "HTTP"

    matcher = "200"

    interval = 30
    timeout  = 5

    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  tags = {
    Name        = "clothflow-grafana-tg"
    Project     = "clothflow"
    Service     = "grafana"
    Environment = "dev"
  }
}

# =========================================================
# Grafana Routing Rule
# =========================================================

resource "aws_lb_listener_rule" "grafana" {
  listener_arn = aws_lb_listener.http.arn

  priority = 400

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.grafana.arn
  }

  condition {
    path_pattern {
      values = [
        "/grafana",
        "/grafana/*"
      ]
    }
  }
}