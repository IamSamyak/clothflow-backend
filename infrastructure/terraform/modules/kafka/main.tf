resource "aws_security_group" "kafka" {
  name        = "clothflow-kafka-sg"
  description = "Security group for ClothFlow Kafka"
  vpc_id      = var.vpc_id

  tags = {
    Name        = "clothflow-kafka-sg"
    Project     = "clothflow"
    Environment = "dev"
  }
}

resource "aws_vpc_security_group_ingress_rule" "kafka_from_app" {
  security_group_id = aws_security_group.kafka.id

  referenced_security_group_id = var.app_security_group_id

  from_port   = 9092
  to_port     = 9092
  ip_protocol = "tcp"

  description = "Kafka access from ECS application services"
}

resource "aws_msk_cluster" "this" {
  cluster_name           = "clothflow-kafka"
  kafka_version          = var.kafka_version
  number_of_broker_nodes = 1

  broker_node_group_info {
    instance_type = "kafka.t3.small"

    client_subnets = var.app_subnet_ids

    security_groups = [
      aws_security_group.kafka.id
    ]

    storage_info {
      ebs_storage_info {
        volume_size = 20
      }
    }
  }

  encryption_info {
    encryption_in_transit {
      client_broker = "PLAINTEXT"
      in_cluster    = true
    }
  }

  tags = {
    Name        = "clothflow-kafka"
    Project     = "clothflow"
    Environment = "dev"
  }
}
