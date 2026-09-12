terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }
}

provider "aws" {
  region  = "us-east-1"
  profile = "clothflow"
}

module "vpc" {
  source = "../../modules/vpc"

  vpc_cidr = "10.20.0.0/16"

  availability_zones = [
    "us-east-1a",
    "us-east-1b"
  ]
}

module "alb" {
  source = "../../modules/alb"

  vpc_id = module.vpc.vpc_id

  public_subnet_ids = module.vpc.public_subnet_ids

  security_group_id = module.vpc.alb_security_group_id
}

module "ecr" {
  source = "../../modules/ecr"
}

module "rds" {
  source = "../../modules/rds"

  vpc_id = module.vpc.vpc_id

  db_subnet_ids = module.vpc.db_subnet_ids

  security_group_id = module.vpc.db_security_group_id
}

# =========================================================
# User Service JWT Secret
# =========================================================

resource "aws_secretsmanager_secret" "user_jwt_keys" {
  name = "clothflow/user/jwt"

  description = "Shared RSA JWT key set for ClothFlow User Service"

  tags = {
    Project     = "clothflow"
    Environment = "dev"
    Service     = "user-service"
    Purpose     = "jwt-signing-keys"
  }
}

module "iam" {
  source = "../../modules/iam"

  rds_secret_arn = module.rds.master_user_secret_arn

  shipping_webhook_secret_arn = "arn:aws:secretsmanager:us-east-1:000000000000:secret:clothflow/shipping/webhook-6EK72O"

  jwt_secret_arn = aws_secretsmanager_secret.user_jwt_keys.arn

  service_credentials_secret_arns = [
    aws_secretsmanager_secret.order_service_credentials.arn,
    aws_secretsmanager_secret.payment_service_credentials.arn,
    aws_secretsmanager_secret.shipping_service_credentials.arn
  ]

  user_outbox_encryption_secret_arn = aws_secretsmanager_secret.user_outbox_encryption.arn

  grafana_admin_secret_arn = aws_secretsmanager_secret.grafana_admin.arn
}

module "cloudwatch" {
  source = "../../modules/cloudwatch"

  environment = "dev"
}

# module "service_discovery" {
#   source = "../../modules/service-discovery"

#   vpc_id = module.vpc.vpc_id
# }

module "kafka" {
  source = "../../modules/kafka"

  vpc_id = module.vpc.vpc_id

  app_subnet_ids = module.vpc.app_subnet_ids

  app_security_group_id = module.vpc.app_security_group_id

  bootstrap_brokers_override = "floci-msk-1239c7:9092"
}

resource "aws_secretsmanager_secret" "user_outbox_encryption" {
  name = "clothflow/user/outbox-encryption"

  description = "AES-256 encryption key for sensitive User Service outbox payloads"

  tags = {
    Project     = "clothflow"
    Environment = "dev"
    Service     = "user-service"
    Purpose     = "outbox-encryption"
  }
}

module "redis" {

  source = "../../modules/redis"

  replication_group_id = "clothflow-redis"

  description = "ClothFlow Redis for User Service rate limiting"

  subnet_ids = module.vpc.app_subnet_ids

  security_group_ids = [
    module.vpc.redis_security_group_id
  ]
}

module "ecs" {
  source = "../../modules/ecs"

  execution_role_arn = module.iam.ecs_execution_role_arn
  task_role_arn      = module.iam.ecs_task_role_arn

  rds_secret_arn = module.rds.master_user_secret_arn

  app_subnet_ids        = module.vpc.app_subnet_ids
  app_security_group_id = module.vpc.app_security_group_id

  # service_discovery_namespace_id = module.service_discovery.namespace_id

  shipping_webhook_secret_arn = "arn:aws:secretsmanager:us-east-1:000000000000:secret:clothflow/shipping/webhook-6EK72O"


  # =========================================================
  # Product Service
  # =========================================================

  product_container_image  = var.product_container_image
  product_log_group_name   = module.cloudwatch.product_service_log_group_name
  product_target_group_arn = module.alb.target_group_arn

  product_db_host = "floci-rds-db-31286345FB12428681BD7C8C-fb3e3f"
  product_db_port = "5432"
  product_db_name = module.rds.db_name


  # =========================================================
  # Inventory Service
  # =========================================================

  inventory_container_image  = var.inventory_container_image
  inventory_log_group_name   = module.cloudwatch.inventory_service_log_group_name
  inventory_target_group_arn = module.alb.inventory_target_group_arn

  inventory_db_host = "floci-rds-db-31286345FB12428681BD7C8C-fb3e3f"
  inventory_db_port = "5432"
  inventory_db_name = "clothflow_inventory"

  # =========================================================
  # Payment Service
  # =========================================================

  payment_container_image  = var.payment_container_image
  payment_log_group_name   = module.cloudwatch.payment_service_log_group_name
  payment_target_group_arn = module.alb.payment_target_group_arn

  payment_db_host = "floci-rds-db-31286345FB12428681BD7C8C-fb3e3f"
  payment_db_port = "5432"
  payment_db_name = "clothflow_payment"

  # Order

  order_container_image = var.order_container_image

  order_log_group_name = module.cloudwatch.order_service_log_group_name

  order_target_group_arn = module.alb.order_target_group_arn

  order_db_host = "floci-rds-db-31286345FB12428681BD7C8C-fb3e3f"

  order_db_port = "5432"

  order_db_name = "clothflow_order"




  shipping_container_image = var.shipping_container_image

  shipping_log_group_name = module.cloudwatch.shipping_service_log_group_name

  shipping_target_group_arn = module.alb.shipping_target_group_arn

  shipping_db_host = "floci-rds-db-31286345FB12428681BD7C8C-fb3e3f"
  shipping_db_port = "5432"
  shipping_db_name = "clothflow_shipping"


  notification_container_image = var.notification_container_image

  notification_log_group_name = module.cloudwatch.notification_service_log_group_name

  notification_target_group_arn = module.alb.notification_target_group_arn

  notification_db_host = "floci-rds-db-31286345FB12428681BD7C8C-fb3e3f"
  notification_db_port = "5432"
  notification_db_name = "clothflow_notification"

  # =========================================================
  # User Service
  # =========================================================

  user_container_image = var.user_container_image

  user_log_group_name = module.cloudwatch.user_service_log_group_name

  user_target_group_arn = module.alb.user_target_group_arn

  user_db_host = "floci-rds-db-31286345FB12428681BD7C8C-fb3e3f"
  user_db_port = "5432"
  user_db_name = "clothflow_user"

  user_task_role_arn = module.iam.user_service_task_role_arn

  user_outbox_encryption_secret_arn = aws_secretsmanager_secret.user_outbox_encryption.arn

  jwt_secret_arn = aws_secretsmanager_secret.user_jwt_keys.arn

  order_service_credentials_secret_arn = aws_secretsmanager_secret.order_service_credentials.arn

  payment_service_credentials_secret_arn = aws_secretsmanager_secret.payment_service_credentials.arn

  shipping_service_credentials_secret_arn = aws_secretsmanager_secret.shipping_service_credentials.arn

  prometheus_container_image = "${module.ecr.prometheus_repository_url}:1.0.5"

  observability_security_group_id = module.vpc.observability_security_group_id

  prometheus_log_group_name = module.cloudwatch.prometheus_log_group_name


  grafana_container_image = "${module.ecr.grafana_repository_url}:1.0.3"

  grafana_log_group_name = module.cloudwatch.grafana_log_group_name

  grafana_efs_file_system_id = module.efs.file_system_id

  prometheus_service_host = "floci-ecs-prometheus"

  grafana_admin_secret_arn = aws_secretsmanager_secret.grafana_admin.arn

  grafana_target_group_arn = module.alb.grafana_target_group_arn

  grafana_root_url = "http://${module.alb.alb_dns_name}/grafana/"


  # =========================================================
  # Kafka
  # =========================================================

  kafka_bootstrap_servers = module.kafka.bootstrap_brokers

  # FLOCI ElastiCache exposes localhost through the AWS API,
  # but ECS tasks communicate with the actual Valkey container
  # through the shared Docker network.
  user_redis_host = "floci-valkey-clothflow-redis"

  user_redis_port = module.redis.primary_endpoint_port

  alb_dns_name = module.alb.alb_dns_name

}

resource "aws_secretsmanager_secret" "order_service_credentials" {
  name        = "clothflow/service/order"
  description = "Service credentials for ClothFlow Order Service"

  tags = {
    Project     = "clothflow"
    Environment = "dev"
    Service     = "order-service"
    Purpose     = "service-authentication"
  }
}

resource "aws_secretsmanager_secret" "payment_service_credentials" {
  name        = "clothflow/service/payment"
  description = "Service credentials for ClothFlow Payment Service"

  tags = {
    Project     = "clothflow"
    Environment = "dev"
    Service     = "payment-service"
    Purpose     = "service-authentication"
  }
}

resource "aws_secretsmanager_secret" "shipping_service_credentials" {
  name        = "clothflow/service/shipping"
  description = "Service credentials for ClothFlow Shipping Service"

  tags = {
    Project     = "clothflow"
    Environment = "dev"
    Service     = "shipping-service"
    Purpose     = "service-authentication"
  }
}

module "efs" {
  source = "../../modules/efs"

  name = "clothflow-grafana"

  subnet_ids = module.vpc.app_subnet_ids

  security_group_id = module.vpc.efs_security_group_id

  environment = "dev"
}

# =========================================================
# Grafana Admin Credentials
# =========================================================

resource "aws_secretsmanager_secret" "grafana_admin" {
  name = "clothflow/grafana/admin"

  description = "Admin credentials for ClothFlow Grafana"

  tags = {
    Project     = "clothflow"
    Environment = "dev"
    Service     = "grafana"
    Purpose     = "grafana-admin-credentials"
  }
}

module "github_actions" {
  source = "../../modules/github-actions"

  github_repository = "IamSamyak/clothflow-backend"
  github_branch     = "main"
  environment       = "dev"

  ecs_execution_role_arn = module.iam.ecs_execution_role_arn

  ecs_task_role_arns = [
    module.iam.ecs_task_role_arn
  ]
}