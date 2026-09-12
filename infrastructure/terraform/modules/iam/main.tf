resource "aws_iam_role" "ecs_execution" {
  name = "clothflow-ecs-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"

    Statement = [
      {
        Effect = "Allow"

        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }

        Action = "sts:AssumeRole"
      }
    ]
  })

  tags = {
    Project     = "clothflow"
    Environment = "dev"
  }
}

resource "aws_iam_role_policy_attachment" "ecs_execution" {
  role       = aws_iam_role.ecs_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}


resource "aws_iam_role" "ecs_task" {
  name = "clothflow-ecs-task-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"

    Statement = [
      {
        Effect = "Allow"

        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }

        Action = "sts:AssumeRole"
      }
    ]
  })

  tags = {
    Project     = "clothflow"
    Environment = "dev"
  }
}

resource "aws_iam_role_policy" "ecs_execution_secrets" {
  name = "clothflow-ecs-read-secrets"

  role = aws_iam_role.ecs_execution.id

  policy = jsonencode({
    Version = "2012-10-17"

    Statement = [
      {
        Effect = "Allow"

        Action = [
          "secretsmanager:GetSecretValue"
        ]

       Resource = concat(
          [
            var.rds_secret_arn,
            var.shipping_webhook_secret_arn,
            var.jwt_secret_arn,
            var.user_outbox_encryption_secret_arn,
            var.grafana_admin_secret_arn
          ],
          var.service_credentials_secret_arns
        )
      }
    ]
  })
}

# =========================================================
# User Service Task Role
# =========================================================

resource "aws_iam_role" "user_service_task" {
  name = "clothflow-user-service-task-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"

    Statement = [
      {
        Effect = "Allow"

        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }

        Action = "sts:AssumeRole"
      }
    ]
  })

  tags = {
    Project     = "clothflow"
    Environment = "dev"
    Service     = "user-service"
  }
}

resource "aws_iam_role_policy" "user_service_jwt_secret" {
  name = "clothflow-user-service-read-jwt-secret"

  role = aws_iam_role.user_service_task.id

  policy = jsonencode({
    Version = "2012-10-17"

    Statement = [
      {
        Effect = "Allow"

        Action = [
          "secretsmanager:GetSecretValue"
        ]

        Resource = var.jwt_secret_arn
      }
    ]
  })
}

resource "aws_iam_role_policy" "user_service_service_credentials" {
  name = "clothflow-user-service-read-service-credentials"

  role = aws_iam_role.user_service_task.id

  policy = jsonencode({
    Version = "2012-10-17"

    Statement = [{
      Effect = "Allow"

      Action = [
        "secretsmanager:GetSecretValue"
      ]

      Resource = var.service_credentials_secret_arns
    }]
  })
}