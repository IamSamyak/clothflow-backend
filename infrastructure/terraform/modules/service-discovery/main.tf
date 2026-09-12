resource "aws_service_discovery_private_dns_namespace" "clothflow" {
  name = "clothflow.local"

  description = "Private DNS namespace for ClothFlow ECS services"

  vpc = var.vpc_id

  tags = {
    Project     = "clothflow"
    Environment = "dev"
  }
}