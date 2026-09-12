resource "aws_ecs_cluster" "this" {
  name = "clothflow"

  tags = {
    Project     = "clothflow"
    Environment = "dev"
  }
}

# =========================================================
# Cloud Map Service Discovery
# =========================================================

resource "aws_service_discovery_service" "product_service" {
  name = "product-service"

  # namespace_id = var.service_discovery_namespace_id
#
# dns_config {
#   namespace_id = var.service_discovery_namespace_id
#
#   dns_records {
#     ttl  = 10
#     type = "A"
#   }
#
#   routing_policy = "MULTIVALUE"
# }


  # health_check_custom_config {
  #   failure_threshold = 1
  # }

  tags = {
    Project     = "clothflow"
    Environment = "dev"
    Service     = "product-service"
  }
}

# resource "aws_service_discovery_service" "inventory_service" {
#   name = "inventory-service"

#   # namespace_id = var.service_discovery_namespace_id
# #
# # dns_config {
# #   namespace_id = var.service_discovery_namespace_id
# #
# #   dns_records {
# #     ttl  = 10
# #     type = "A"
# #   }
# #
# #   routing_policy = "MULTIVALUE"
# # }


#   # health_check_custom_config {
#   #   failure_threshold = 1
#   # }

#   tags = {
#     Project     = "clothflow"
#     Environment = "dev"
#     Service     = "inventory-service"
#   }
# }

resource "aws_service_discovery_service" "payment_service" {
  name = "payment-service"

  # namespace_id = var.service_discovery_namespace_id
#
# dns_config {
#   namespace_id = var.service_discovery_namespace_id
#
#   dns_records {
#     ttl  = 10
#     type = "A"
#   }
#
#   routing_policy = "MULTIVALUE"
# }


  

  tags = {
    Project     = "clothflow"
    Environment = "dev"
    Service     = "payment-service"
  }
}

resource "aws_service_discovery_service" "order_service" {
  name = "order-service"

  # namespace_id = var.service_discovery_namespace_id
#
# dns_config {
#   namespace_id = var.service_discovery_namespace_id
#
#   dns_records {
#     ttl  = 10
#     type = "A"
#   }
#
#   routing_policy = "MULTIVALUE"
# }


  

  tags = {
    Project     = "clothflow"
    Environment = "dev"
    Service     = "order-service"
  }
}

# =========================================================
# Product Service
# =========================================================

resource "aws_ecs_task_definition" "product_service" {
  family = "clothflow-product-service"

  network_mode = "awsvpc"

  requires_compatibilities = ["FARGATE"]

  cpu    = "256"
  memory = "512"

  execution_role_arn = var.execution_role_arn
  task_role_arn      = var.task_role_arn

  container_definitions = jsonencode([
    {
      name      = "product-service"
      image     = var.product_container_image
      essential = true

      environment = [
        {
          name  = "DB_HOST"
          value = var.product_db_host
        },
        {
          name  = "DB_PORT"
          value = var.product_db_port
        },
        {
          name  = "DB_NAME"
          value = var.product_db_name
        },
        {
          name  = "JWT_JWK_SET_URI"
          value = "http://${var.alb_dns_name}/.well-known/jwks.json"
        }
      ]

      secrets = [
        {
          name      = "DB_USERNAME"
          valueFrom = "${var.rds_secret_arn}:username::"
        },
        {
          name      = "DB_PASSWORD"
          valueFrom = "${var.rds_secret_arn}:password::"
        }
      ]

      portMappings = [
        {
          containerPort = 8080
          hostPort      = 8080
          protocol      = "tcp"
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"

        options = {
          "awslogs-group"         = var.product_log_group_name
          "awslogs-region"        = "us-east-1"
          "awslogs-stream-prefix" = "product-service"
        }
      }
    }
  ])

  tags = {
    Project     = "clothflow"
    Environment = "dev"
    Service     = "product-service"
  }
}

resource "aws_ecs_service" "product_service" {
  name            = "clothflow-product-service"
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.product_service.arn

  desired_count = 1

  launch_type = "FARGATE"

  deployment_controller {
    type = "ECS"
  }

  force_new_deployment = true

  # service_registries {
  #   registry_arn = aws_service_discovery_service.product_service.arn
  #   port         = 8080
  # }

  network_configuration {
    subnets = var.app_subnet_ids

    security_groups = [
      var.app_security_group_id
    ]

    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = var.product_target_group_arn
    container_name   = "product-service"
    container_port   = 8080
  }

  tags = {
    Project     = "clothflow"
    Environment = "dev"
    Service     = "product-service"
  }
}

# =========================================================
# Inventory Service
# =========================================================

resource "aws_ecs_task_definition" "inventory_service" {
  family = "clothflow-inventory-service"

  network_mode = "awsvpc"

  requires_compatibilities = ["FARGATE"]

  cpu    = "256"
  memory = "512"

  execution_role_arn = var.execution_role_arn
  task_role_arn      = var.task_role_arn

  container_definitions = jsonencode([
    {
      name      = "inventory-service"
      image     = var.inventory_container_image
      essential = true

      environment = [
        {
          name  = "SPRING_DATASOURCE_URL"
          value = "jdbc:postgresql://${var.inventory_db_host}:${var.inventory_db_port}/${var.inventory_db_name}"
        },
        {
          name  = "SPRING_KAFKA_BOOTSTRAP_SERVERS"
          value = var.kafka_bootstrap_servers
        },
        {
          name  = "SERVER_PORT"
          value = "8082"
        },
        {
          name  = "JWT_JWK_SET_URI"
          value = "http://${var.alb_dns_name}/.well-known/jwks.json"
        }
      ]

      secrets = [
        {
          name      = "SPRING_DATASOURCE_USERNAME"
          valueFrom = "${var.rds_secret_arn}:username::"
        },
        {
          name      = "SPRING_DATASOURCE_PASSWORD"
          valueFrom = "${var.rds_secret_arn}:password::"
        }
      ]

      portMappings = [
        {
          containerPort = 8082
          hostPort      = 8082
          protocol      = "tcp"
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"

        options = {
          "awslogs-group"         = var.inventory_log_group_name
          "awslogs-region"        = "us-east-1"
          "awslogs-stream-prefix" = "inventory-service"
        }
      }
    }
  ])

  tags = {
    Project     = "clothflow"
    Environment = "dev"
    Service     = "inventory-service"
  }
}

resource "aws_ecs_service" "inventory_service" {
  name            = "clothflow-inventory-service"
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.inventory_service.arn

  desired_count = 1

  launch_type = "FARGATE"

  deployment_controller {
    type = "ECS"
  }

  force_new_deployment = true

  # service_registries {
  #   registry_arn = aws_service_discovery_service.inventory_service.arn
  #   port         = 8082
  # }

  network_configuration {
    subnets = var.app_subnet_ids

    security_groups = [
      var.app_security_group_id
    ]

    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = var.inventory_target_group_arn
    container_name   = "inventory-service"
    container_port   = 8082
  }

  tags = {
    Project     = "clothflow"
    Environment = "dev"
    Service     = "inventory-service"
  }
}

# =========================================================
# Payment Service
# =========================================================

resource "aws_ecs_task_definition" "payment_service" {
  family = "clothflow-payment-service"

  network_mode = "awsvpc"

  requires_compatibilities = ["FARGATE"]

  cpu    = "256"
  memory = "512"

  execution_role_arn = var.execution_role_arn
  task_role_arn      = var.task_role_arn

  container_definitions = jsonencode([
    {
      name      = "payment-service"
      image     = var.payment_container_image
      essential = true

      environment = [
        {
          name  = "SPRING_DATASOURCE_URL"
          value = "jdbc:postgresql://${var.payment_db_host}:${var.payment_db_port}/${var.payment_db_name}"
        },
        {
          name  = "SPRING_KAFKA_BOOTSTRAP_SERVERS"
          value = var.kafka_bootstrap_servers
        },
        {
          name  = "SERVER_PORT"
          value = "8084"
        },
        {
          name  = "JWT_JWK_SET_URI"
          value = "http://${var.alb_dns_name}/.well-known/jwks.json"
        }
      ]

      secrets = [
        {
          name      = "SPRING_DATASOURCE_USERNAME"
          valueFrom = "${var.rds_secret_arn}:username::"
        },
        {
          name      = "SPRING_DATASOURCE_PASSWORD"
          valueFrom = "${var.rds_secret_arn}:password::"
        }
      ]

      portMappings = [
        {
          containerPort = 8084
          hostPort      = 8084
          protocol      = "tcp"
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"

        options = {
          "awslogs-group"         = var.payment_log_group_name
          "awslogs-region"        = "us-east-1"
          "awslogs-stream-prefix" = "payment-service"
        }
      }
    }
  ])

  tags = {
    Project     = "clothflow"
    Environment = "dev"
    Service     = "payment-service"
  }
}

resource "aws_ecs_service" "payment_service" {
  name            = "clothflow-payment-service"
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.payment_service.arn

  desired_count = 1

  launch_type = "FARGATE"

  deployment_controller {
    type = "ECS"
  }

  force_new_deployment = true

  # service_registries {
  #   registry_arn = aws_service_discovery_service.payment_service.arn
  #   port         = 8084
  # }

  network_configuration {
    subnets = var.app_subnet_ids

    security_groups = [
      var.app_security_group_id
    ]

    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = var.payment_target_group_arn
    container_name   = "payment-service"
    container_port   = 8084
  }

  tags = {
    Project     = "clothflow"
    Environment = "dev"
    Service     = "payment-service"
  }
}

# =========================================================
# Order Service
# =========================================================

resource "aws_ecs_task_definition" "order_service" {
  family = "clothflow-order-service"

  network_mode = "awsvpc"

  requires_compatibilities = ["FARGATE"]

  cpu    = "256"
  memory = "512"

  execution_role_arn = var.execution_role_arn
  task_role_arn      = var.task_role_arn

  container_definitions = jsonencode([
    {
      name      = "order-service"
      image     = var.order_container_image
      essential = true

      environment = [
        {
          name  = "SPRING_DATASOURCE_URL"
          value = "jdbc:postgresql://${var.order_db_host}:${var.order_db_port}/${var.order_db_name}"
        },
        {
          name  = "SPRING_KAFKA_BOOTSTRAP_SERVERS"
          value = var.kafka_bootstrap_servers
        },
        {
          name  = "PRODUCT_SERVICE_BASE_URL"
          value = "http://floci"
        },
        {
          name  = "INVENTORY_SERVICE_BASE_URL"
          value = "http://floci"
        },
        {
          name  = "PAYMENT_SERVICE_BASE_URL"
          value = "http://floci"
        },
        {
          name  = "SERVER_PORT"
          value = "8083"
        },
        {
          name  = "USER_SERVICE_BASE_URL"
          value = "http://${var.alb_dns_name}"
        },
        {
          name  = "JWT_JWK_SET_URI"
          value = "http://${var.alb_dns_name}/.well-known/jwks.json"
        }
      ]

      secrets = [
      {
        name      = "SPRING_DATASOURCE_USERNAME"
        valueFrom = "${var.rds_secret_arn}:username::"
      },
      {
        name      = "SPRING_DATASOURCE_PASSWORD"
        valueFrom = "${var.rds_secret_arn}:password::"
      },
      {
        name      = "ORDER_SERVICE_CLIENT_ID"
        valueFrom = "${var.order_service_credentials_secret_arn}:clientId::"
      },
      {
        name      = "ORDER_SERVICE_CLIENT_SECRET"
        valueFrom = "${var.order_service_credentials_secret_arn}:clientSecret::"
      }
    ]

      portMappings = [
        {
          containerPort = 8083
          hostPort      = 8083
          protocol      = "tcp"
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"

        options = {
          "awslogs-group"         = var.order_log_group_name
          "awslogs-region"        = "us-east-1"
          "awslogs-stream-prefix" = "order-service"
        }
      }
    }
  ])

  tags = {
    Project     = "clothflow"
    Environment = "dev"
    Service     = "order-service"
  }
}

resource "aws_ecs_service" "order_service" {
  name            = "clothflow-order-service"
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.order_service.arn

  desired_count = 1

  launch_type = "FARGATE"

  deployment_controller {
    type = "ECS"
  }

  force_new_deployment = true

  # service_registries {
  #   registry_arn = aws_service_discovery_service.order_service.arn
  #   port         = 8083
  # }

  network_configuration {
    subnets = var.app_subnet_ids

    security_groups = [
      var.app_security_group_id
    ]

    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = var.order_target_group_arn
    container_name   = "order-service"
    container_port   = 8083
  }

  tags = {
    Project     = "clothflow"
    Environment = "dev"
    Service     = "order-service"
  }
}

resource "aws_ecs_task_definition" "shipping_service" {
  family                   = "clothflow-shipping-service"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]

  cpu    = "512"
  memory = "1024"

  execution_role_arn = var.execution_role_arn
  task_role_arn      = var.task_role_arn

  container_definitions = jsonencode([
    {
      name      = "shipping-service"
      image     = var.shipping_container_image
      essential = true

      portMappings = [
        {
          containerPort = 8085
          hostPort      = 8085
          protocol      = "tcp"
        }
      ]

      environment = [
        {
          name  = "SERVER_PORT"
          value = "8085"
        },
        {
          name  = "SPRING_DATASOURCE_URL"
          value = "jdbc:postgresql://${var.shipping_db_host}:${var.shipping_db_port}/${var.shipping_db_name}"
        },
        {
          name  = "SPRING_KAFKA_BOOTSTRAP_SERVERS"
          value = var.kafka_bootstrap_servers
        },
        {
          name  = "JWT_JWK_SET_URI"
          value = "http://${var.alb_dns_name}/.well-known/jwks.json"
        }
      ]

      secrets = [
        {
          name      = "SPRING_DATASOURCE_USERNAME"
          valueFrom = "${var.rds_secret_arn}:username::"
        },
        {
          name      = "SPRING_DATASOURCE_PASSWORD"
          valueFrom = "${var.rds_secret_arn}:password::"
        },
        {
          name      = "SHIPPING_WEBHOOK_SECRET"
          valueFrom = var.shipping_webhook_secret_arn
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"

        options = {
          awslogs-group         = var.shipping_log_group_name
          awslogs-region        = "us-east-1"
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])

  tags = {
    Project     = "clothflow"
    Environment = "dev"
    Service     = "shipping-service"
  }
}


resource "aws_ecs_service" "shipping_service" {
  name            = "clothflow-shipping-service"
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.shipping_service.arn

  desired_count = 1

  launch_type = "FARGATE"

  network_configuration {
    subnets          = var.app_subnet_ids
    security_groups  = [var.app_security_group_id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = var.shipping_target_group_arn
    container_name   = "shipping-service"
    container_port   = 8085
  }

  # service_registries {
  #   registry_arn = aws_service_discovery_service.shipping_service.arn
  #   port         = 8085
  # }

  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200

  depends_on = [
    aws_ecs_task_definition.shipping_service,
    # aws_service_discovery_service.shipping_service
  ]

  tags = {
    Project     = "clothflow"
    Environment = "dev"
    Service     = "shipping-service"
  }
}

resource "aws_service_discovery_service" "shipping_service" {
  name = "shipping-service"

  # namespace_id = var.service_discovery_namespace_id

  # dns_config {
  #   namespace_id = var.service_discovery_namespace_id

  #   routing_policy = "MULTIVALUE"

  #   dns_records {
  #     ttl  = 10
  #     type = "A"
  #   }
  # }

  

  tags = {
    Project     = "clothflow"
    Environment = "dev"
    Service     = "shipping-service"
  }
}

resource "aws_service_discovery_service" "notification_service" {
  name = "notification-service"

  # namespace_id = var.service_discovery_namespace_id

  # dns_config {
  #   namespace_id = var.service_discovery_namespace_id

  #   routing_policy = "MULTIVALUE"

  #   dns_records {
  #     ttl  = 10
  #     type = "A"
  #   }
  # }

  

  tags = {
    Project     = "clothflow"
    Environment = "dev"
    Service     = "notification-service"
  }
}

resource "aws_ecs_task_definition" "notification_service" {
  family                   = "clothflow-notification-service"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]

  cpu    = "512"
  memory = "1024"

  execution_role_arn = var.execution_role_arn
  task_role_arn      = var.task_role_arn

  container_definitions = jsonencode([
    {
      name      = "notification-service"
      image     = var.notification_container_image
      essential = true

      portMappings = [
        {
          containerPort = 8086
          hostPort      = 8086
          protocol      = "tcp"
        }
      ]

      environment = [
        {
          name  = "SERVER_PORT"
          value = "8086"
        },
        {
          name  = "SPRING_DATASOURCE_URL"
          value = "jdbc:postgresql://${var.notification_db_host}:${var.notification_db_port}/${var.notification_db_name}"
        },
        {
          name  = "SPRING_KAFKA_BOOTSTRAP_SERVERS"
          value = var.kafka_bootstrap_servers
        },
        {
          name  = "SPRING_KAFKA_CONSUMER_GROUP_ID"
          value = "clothflow-notification-service"
        },
        {
          name  = "JWT_JWK_SET_URI"
          value = "http://${var.alb_dns_name}/.well-known/jwks.json"
        }
      ]

      secrets = [
        {
          name      = "SPRING_DATASOURCE_USERNAME"
          valueFrom = "${var.rds_secret_arn}:username::"
        },
        {
          name      = "SPRING_DATASOURCE_PASSWORD"
          valueFrom = "${var.rds_secret_arn}:password::"
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"

        options = {
          awslogs-group         = var.notification_log_group_name
          awslogs-region        = "us-east-1"
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])

  tags = {
    Project     = "clothflow"
    Environment = "dev"
    Service     = "notification-service"
  }
}

resource "aws_ecs_service" "notification_service" {
  name            = "clothflow-notification-service"
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.notification_service.arn

  desired_count = 1

  launch_type = "FARGATE"

  network_configuration {
    subnets          = var.app_subnet_ids
    security_groups  = [var.app_security_group_id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = var.notification_target_group_arn
    container_name   = "notification-service"
    container_port   = 8086
  }

  # service_registries {
  #   registry_arn = aws_service_discovery_service.notification_service.arn
  #   port         = 8086
  # }

  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200

  depends_on = [
    aws_ecs_task_definition.notification_service,
    # aws_service_discovery_service.notification_service
  ]

  tags = {
    Project     = "clothflow"
    Environment = "dev"
    Service     = "notification-service"
  }
}


# =========================================================
# User Service
# =========================================================

resource "aws_ecs_task_definition" "user_service" {
  family = "clothflow-user-service"

  network_mode = "awsvpc"

  requires_compatibilities = [
    "FARGATE"
  ]

  cpu    = "512"
  memory = "1024"

  execution_role_arn = var.execution_role_arn
  task_role_arn      = var.user_task_role_arn

  container_definitions = jsonencode([
    {
      name      = "user-service"
      image     = var.user_container_image
      essential = true

      environment = [
        {
          name  = "SERVER_PORT"
          value = "8081"
        },

        {
          name  = "SPRING_DATASOURCE_URL"
          value = "jdbc:postgresql://${var.user_db_host}:${var.user_db_port}/${var.user_db_name}"
        },

        {
          name  = "SPRING_KAFKA_BOOTSTRAP_SERVERS"
          value = var.kafka_bootstrap_servers
        },

        # ===================================================
        # JWT
        # ===================================================

        {
          name  = "JWT_KEY_SOURCE"
          value = "aws"
        },

        {
          name  = "JWT_KEY_SECRET_ID"
          value = var.jwt_secret_arn
        },

        {
          name  = "AWS_REGION"
          value = "us-east-1"
        },

        {
          name  = "JWT_ISSUER"
          value = "clothflow-user-service"
        },

        {
          name  = "JWT_AUDIENCE"
          value = "clothflow-api"
        },

        {
          name  = "JWT_ACCESS_TOKEN_EXPIRATION"
          value = "900"
        },

        {
          name  = "JWT_REFRESH_TOKEN_EXPIRATION"
          value = "2592000"
        },

        {
          name  = "JWT_CLOCK_SKEW_SECONDS"
          value = "30"
        },
        {
          name  = "JWT_SERVICE_TOKEN_EXPIRATION"
          value = "300"
        },
        {
          name = "SPRING_DATA_REDIS_HOST"

          value = var.user_redis_host
        },
        {
          name = "SPRING_DATA_REDIS_PORT"

          value = tostring(
            var.user_redis_port
          )
        }
      ]

      secrets = [
        {
          name = "SPRING_DATASOURCE_USERNAME"
          valueFrom = "${var.rds_secret_arn}:username::"
        },
        {
          name = "SPRING_DATASOURCE_PASSWORD"
          valueFrom = "${var.rds_secret_arn}:password::"
        },
        {
          name = "OUTBOX_ENCRYPTION_KEY"
          valueFrom = var.user_outbox_encryption_secret_arn
        },
       {
          name = "ORDER_SERVICE_CLIENT_ID"

          valueFrom = "${var.order_service_credentials_secret_arn}:clientId::"
        },
        {
          name = "ORDER_SERVICE_CLIENT_SECRET"

          valueFrom = "${var.order_service_credentials_secret_arn}:clientSecret::"
        },
        {
          name = "PAYMENT_SERVICE_CLIENT_ID"

          valueFrom = "${var.payment_service_credentials_secret_arn}:clientId::"
        },
        {
          name = "PAYMENT_SERVICE_CLIENT_SECRET"

          valueFrom = "${var.payment_service_credentials_secret_arn}:clientSecret::"
        },
        {
          name = "SHIPPING_SERVICE_CLIENT_ID"

          valueFrom = "${var.shipping_service_credentials_secret_arn}:clientId::"
        },
        {
          name = "SHIPPING_SERVICE_CLIENT_SECRET"

          valueFrom = "${var.shipping_service_credentials_secret_arn}:clientSecret::"
        }
      ]

      portMappings = [
        {
          containerPort = 8081
          hostPort      = 8081
          protocol      = "tcp"
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"

        options = {
          "awslogs-group"         = var.user_log_group_name
          "awslogs-region"        = "us-east-1"
          "awslogs-stream-prefix" = "user-service"
        }
      }
    }
  ])

  tags = {
    Project     = "clothflow"
    Environment = "dev"
    Service     = "user-service"
  }
}

resource "aws_ecs_service" "user_service" {
  name = "clothflow-user-service"

  cluster = aws_ecs_cluster.this.id

  task_definition = aws_ecs_task_definition.user_service.arn

  desired_count = 1

  launch_type = "FARGATE"

  deployment_controller {
    type = "ECS"
  }

  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200

  force_new_deployment = true

  network_configuration {
    subnets = var.app_subnet_ids

    security_groups = [
      var.app_security_group_id
    ]

    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = var.user_target_group_arn

    container_name = "user-service"

    container_port = 8081
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

resource "aws_ecs_task_definition" "prometheus" {
  family = "clothflow-prometheus"

  network_mode = "awsvpc"

  requires_compatibilities = ["FARGATE"]

  cpu    = "512"
  memory = "1024"

  execution_role_arn = var.execution_role_arn
  task_role_arn      = var.task_role_arn

  volume {
    name      = "docker-socket"
    host_path = "/var/run/docker.sock"
  }

  container_definitions = jsonencode([
    {
      name      = "prometheus"
      image     = var.prometheus_container_image
      essential = true


      portMappings = [
        {
          containerPort = 9090
          hostPort      = 9090
          protocol      = "tcp"
        }
      ]

      mountPoints = [
        {
          sourceVolume  = "docker-socket"
          containerPath = "/var/run/docker.sock"
          readOnly      = true
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"

        options = {
          "awslogs-group"         = var.prometheus_log_group_name
          "awslogs-region"        = "us-east-1"
          "awslogs-stream-prefix" = "prometheus"
        }
      }
    }
  ])

  tags = {
    Project     = "clothflow"
    Environment = "dev"
    Service     = "prometheus"
  }
}

resource "aws_ecs_service" "prometheus" {
  name            = "clothflow-prometheus"
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.prometheus.arn

  desired_count = 1

  launch_type = "FARGATE"

  deployment_controller {
    type = "ECS"
  }

  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200

  force_new_deployment = true

  network_configuration {
    subnets = var.app_subnet_ids

    security_groups = [
      var.observability_security_group_id
    ]

    assign_public_ip = false
  }

  tags = {
    Project     = "clothflow"
    Environment = "dev"
    Service     = "prometheus"
  }
}


# =========================================================
# Grafana
# =========================================================

resource "aws_ecs_task_definition" "grafana" {
  family = "clothflow-grafana"

  network_mode = "awsvpc"

  requires_compatibilities = ["FARGATE"]

  cpu    = "512"
  memory = "1024"

  execution_role_arn = var.execution_role_arn
  task_role_arn      = var.task_role_arn

  volume {
    name = "grafana-data"

    efs_volume_configuration {
      file_system_id = var.grafana_efs_file_system_id

      root_directory = "/"

      transit_encryption = "ENABLED"
    }
  }

  container_definitions = jsonencode([
    {
      name      = "grafana"
      image     = var.grafana_container_image
      essential = true

      portMappings = [
        {
          containerPort = 3000
          hostPort      = 3000
          protocol      = "tcp"
        }
      ]

     environment = [
          {
            name  = "GF_SERVER_HTTP_PORT"
            value = "3000"
          },
          {
            name  = "GF_SERVER_ROOT_URL"
            value = var.grafana_root_url
          },
          {
            name  = "GF_SERVER_SERVE_FROM_SUB_PATH"
            value = "true"
          },
          {
            name  = "PROMETHEUS_SERVICE_HOST"
            value = var.prometheus_service_host
          }
        ]

      secrets = [
        {
          name      = "GF_SECURITY_ADMIN_USER"
          valueFrom = "${var.grafana_admin_secret_arn}:username::"
        },
        {
          name      = "GF_SECURITY_ADMIN_PASSWORD"
          valueFrom = "${var.grafana_admin_secret_arn}:password::"
        }
      ]

      mountPoints = [
        {
          sourceVolume  = "grafana-data"
          containerPath = "/var/lib/grafana"
          readOnly      = false
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"

        options = {
          "awslogs-group"         = var.grafana_log_group_name
          "awslogs-region"        = "us-east-1"
          "awslogs-stream-prefix" = "grafana"
        }
      }
    }
  ])

  tags = {
    Project     = "clothflow"
    Environment = "dev"
    Service     = "grafana"
  }
}


resource "aws_ecs_service" "grafana" {
  name            = "clothflow-grafana"
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.grafana.arn

  desired_count = 1

  launch_type = "FARGATE"

  deployment_controller {
    type = "ECS"
  }

  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200

  force_new_deployment = true

  network_configuration {
    subnets = var.app_subnet_ids

    security_groups = [
      var.observability_security_group_id
    ]

    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = var.grafana_target_group_arn
    container_name   = "grafana"
    container_port   = 3000
  }

  tags = {
    Project     = "clothflow"
    Environment = "dev"
    Service     = "grafana"
  }

  depends_on = [
    aws_ecs_task_definition.grafana
  ]
}