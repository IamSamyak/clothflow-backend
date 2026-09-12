resource "aws_vpc" "this" {
  cidr_block           = var.vpc_cidr
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name        = "clothflow-vpc"
    Environment = "dev"
    Project     = "clothflow"
  }
}
resource "aws_subnet" "public_a" {
  vpc_id                  = aws_vpc.this.id
  cidr_block              = "10.20.0.0/20"
  availability_zone       = var.availability_zones[0]
  map_public_ip_on_launch = true

  tags = {
    Name = "clothflow-public-a"
    Tier = "public"
  }
}

resource "aws_subnet" "public_b" {
  vpc_id                  = aws_vpc.this.id
  cidr_block              = "10.20.48.0/20"
  availability_zone       = var.availability_zones[1]
  map_public_ip_on_launch = true

  tags = {
    Name = "clothflow-public-b"
    Tier = "public"
  }
}

resource "aws_subnet" "app_a" {
  vpc_id            = aws_vpc.this.id
  cidr_block        = "10.20.16.0/20"
  availability_zone = var.availability_zones[0]

  tags = {
    Name = "clothflow-app-a"
    Tier = "private-app"
  }
}

resource "aws_subnet" "app_b" {
  vpc_id            = aws_vpc.this.id
  cidr_block        = "10.20.64.0/20"
  availability_zone = var.availability_zones[1]

  tags = {
    Name = "clothflow-app-b"
    Tier = "private-app"
  }
}

resource "aws_subnet" "db_a" {
  vpc_id            = aws_vpc.this.id
  cidr_block        = "10.20.32.0/20"
  availability_zone = var.availability_zones[0]

  tags = {
    Name = "clothflow-db-a"
    Tier = "private-db"
  }
}

resource "aws_subnet" "db_b" {
  vpc_id            = aws_vpc.this.id
  cidr_block        = "10.20.80.0/20"
  availability_zone = var.availability_zones[1]

  tags = {
    Name = "clothflow-db-b"
    Tier = "private-db"
  }
}

resource "aws_internet_gateway" "this" {
  vpc_id = aws_vpc.this.id

  tags = {
    Name = "clothflow-igw"
  }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.this.id

  tags = {
    Name = "clothflow-public-rt"
  }
}

resource "aws_route" "public_internet" {
  route_table_id         = aws_route_table.public.id
  destination_cidr_block = "0.0.0.0/0"
  gateway_id             = aws_internet_gateway.this.id
}

resource "aws_route_table_association" "public_a" {
  subnet_id      = aws_subnet.public_a.id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table_association" "public_b" {
  subnet_id      = aws_subnet.public_b.id
  route_table_id = aws_route_table.public.id
}

resource "aws_eip" "nat_a" {
  domain = "vpc"

  tags = {
    Name    = "clothflow-nat-eip-a"
    Project = "clothflow"
  }
}

resource "aws_eip" "nat_b" {
  domain = "vpc"

  tags = {
    Name    = "clothflow-nat-eip-b"
    Project = "clothflow"
  }
}

resource "aws_nat_gateway" "a" {
  allocation_id = aws_eip.nat_a.id
  subnet_id     = aws_subnet.public_a.id

  tags = {
    Name    = "clothflow-nat-a"
    Project = "clothflow"
  }

  depends_on = [aws_internet_gateway.this]
}

resource "aws_nat_gateway" "b" {
  allocation_id = aws_eip.nat_b.id
  subnet_id     = aws_subnet.public_b.id

  tags = {
    Name    = "clothflow-nat-b"
    Project = "clothflow"
  }

  depends_on = [aws_internet_gateway.this]
}

resource "aws_route_table" "app_a" {
  vpc_id = aws_vpc.this.id

  tags = {
    Name    = "clothflow-app-a-rt"
    Project = "clothflow"
  }
}

resource "aws_route_table" "app_b" {
  vpc_id = aws_vpc.this.id

  tags = {
    Name    = "clothflow-app-b-rt"
    Project = "clothflow"
  }
}

resource "aws_route" "app_a_internet" {
  route_table_id         = aws_route_table.app_a.id
  destination_cidr_block = "0.0.0.0/0"
  nat_gateway_id         = aws_nat_gateway.a.id
}

resource "aws_route" "app_b_internet" {
  route_table_id         = aws_route_table.app_b.id
  destination_cidr_block = "0.0.0.0/0"
  nat_gateway_id         = aws_nat_gateway.b.id
}

resource "aws_route_table_association" "app_a" {
  subnet_id      = aws_subnet.app_a.id
  route_table_id = aws_route_table.app_a.id
}

resource "aws_route_table_association" "app_b" {
  subnet_id      = aws_subnet.app_b.id
  route_table_id = aws_route_table.app_b.id
}

resource "aws_security_group" "alb" {
  name        = "clothflow-alb-sg"
  description = "Security group for ClothFlow ALB"
  vpc_id      = aws_vpc.this.id

  tags = {
    Name    = "clothflow-alb-sg"
    Project = "clothflow"
  }
}

resource "aws_security_group" "app" {
  name        = "clothflow-app-sg"
  description = "Security group for ClothFlow application services"
  vpc_id      = aws_vpc.this.id

  tags = {
    Name    = "clothflow-app-sg"
    Project = "clothflow"
  }
}

resource "aws_security_group" "db" {
  name        = "clothflow-db-sg"
  description = "Security group for ClothFlow databases"
  vpc_id      = aws_vpc.this.id

  tags = {
    Name    = "clothflow-db-sg"
    Project = "clothflow"
  }
}

# resource "aws_vpc_security_group_ingress_rule" "alb_https" {
#   security_group_id = aws_security_group.alb.id

#   cidr_ipv4   = "0.0.0.0/0"
#   from_port   = 443
#   to_port     = 443
#   ip_protocol = "tcp"

#   description = "HTTPS from Internet"
# }

resource "aws_vpc_security_group_ingress_rule" "alb_http" {
  security_group_id = aws_security_group.alb.id
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 80
  to_port           = 80
  ip_protocol       = "tcp"
  description       = "HTTP from internet"
}

resource "aws_vpc_security_group_ingress_rule" "app_from_alb" {
  security_group_id = aws_security_group.app.id

  referenced_security_group_id = aws_security_group.alb.id

  from_port   = 8080
  to_port     = 8080
  ip_protocol = "tcp"

  description = "Application traffic from ALB"
}

resource "aws_vpc_security_group_ingress_rule" "db_from_app" {
  security_group_id = aws_security_group.db.id

  referenced_security_group_id = aws_security_group.app.id

  from_port   = 5432
  to_port     = 5432
  ip_protocol = "tcp"

  description = "PostgreSQL access from application services"
}

# resource "aws_vpc_security_group_ingress_rule" "db_from_app_7001" {
#   security_group_id = aws_security_group.db.id

#   referenced_security_group_id = aws_security_group.app.id

#   from_port   = 7001
#   to_port     = 7001
#   ip_protocol = "tcp"

#   description = "PostgreSQL access from application services"
# }

resource "aws_vpc_security_group_ingress_rule" "app_from_alb_inventory" {
  security_group_id = aws_security_group.app.id

  referenced_security_group_id = aws_security_group.alb.id

  from_port   = 8082
  to_port     = 8082
  ip_protocol = "tcp"

  description = "Inventory application traffic from ALB"
}

resource "aws_vpc_security_group_ingress_rule" "app_from_alb_order" {
  security_group_id = aws_security_group.app.id

  referenced_security_group_id = aws_security_group.alb.id

  from_port = 8083
  to_port   = 8083

  ip_protocol = "tcp"

  description = "Order application traffic from ALB"
}

resource "aws_vpc_security_group_ingress_rule" "app_from_alb_payment" {
  security_group_id = aws_security_group.app.id

  referenced_security_group_id = aws_security_group.alb.id

  from_port = 8084
  to_port   = 8084

  ip_protocol = "tcp"

  description = "Payment application traffic from ALB"
}

resource "aws_vpc_security_group_ingress_rule" "app_from_alb_shipping" {
  security_group_id            = aws_security_group.app.id
  referenced_security_group_id = aws_security_group.alb.id

  from_port   = 8085
  to_port     = 8085
  ip_protocol = "tcp"

  description = "Shipping application traffic from ALB"
}

resource "aws_vpc_security_group_ingress_rule" "app_from_alb_notification" {
  security_group_id            = aws_security_group.app.id
  referenced_security_group_id = aws_security_group.alb.id

  from_port   = 8086
  to_port     = 8086
  ip_protocol = "tcp"

  description = "Notification application traffic from ALB"
}

# =========================================================
# ECS Application-to-Application Traffic
# =========================================================

resource "aws_vpc_security_group_ingress_rule" "app_from_app_product" {
  security_group_id            = aws_security_group.app.id
  referenced_security_group_id = aws_security_group.app.id

  from_port   = 8080
  to_port     = 8080
  ip_protocol = "tcp"

  description = "Product Service traffic from ClothFlow application services"
}

resource "aws_vpc_security_group_ingress_rule" "app_from_app_inventory" {
  security_group_id            = aws_security_group.app.id
  referenced_security_group_id = aws_security_group.app.id

  from_port   = 8082
  to_port     = 8082
  ip_protocol = "tcp"

  description = "Inventory Service traffic from ClothFlow application services"
}

resource "aws_vpc_security_group_ingress_rule" "app_from_app_payment" {
  security_group_id            = aws_security_group.app.id
  referenced_security_group_id = aws_security_group.app.id

  from_port   = 8084
  to_port     = 8084
  ip_protocol = "tcp"

  description = "Payment Service traffic from ClothFlow application services"
}

resource "aws_vpc_security_group_ingress_rule" "app_from_alb_user" {
  security_group_id            = aws_security_group.app.id
  referenced_security_group_id = aws_security_group.alb.id

  from_port   = 8081
  to_port     = 8081
  ip_protocol = "tcp"

  description = "User application traffic from ALB"
}

# resource "aws_vpc_security_group_ingress_rule" "app_from_app_order" {
#   security_group_id            = aws_security_group.app.id
#   referenced_security_group_id = aws_security_group.app.id

#   from_port   = 8083
#   to_port     = 8083
#   ip_protocol = "tcp"

#   description = "Order Service traffic from ClothFlow application services"
# }

# resource "aws_vpc_security_group_ingress_rule" "app_from_app_shipping" {
#   security_group_id            = aws_security_group.app.id
#   referenced_security_group_id = aws_security_group.app.id

#   from_port   = 8085
#   to_port     = 8085
#   ip_protocol = "tcp"

#   description = "Shipping Service traffic from ClothFlow application services"
# }

# resource "aws_vpc_security_group_ingress_rule" "app_from_app_notification" {
#   security_group_id            = aws_security_group.app.id
#   referenced_security_group_id = aws_security_group.app.id

#   from_port   = 8086
#   to_port     = 8086
#   ip_protocol = "tcp"

#   description = "Notification Service traffic from ClothFlow application services"
# }

resource "aws_security_group" "redis" {

  name = "clothflow-redis-sg"

  description = "Security group for ClothFlow Redis"

  vpc_id = aws_vpc.this.id

  tags = {
    Project     = "clothflow"
    Environment = "dev"
    Service     = "redis"
  }
}

resource "aws_vpc_security_group_ingress_rule" "redis_from_app" {

  security_group_id = aws_security_group.redis.id

  referenced_security_group_id = aws_security_group.app.id

  from_port = 6379

  to_port = 6379

  ip_protocol = "tcp"

  description = "Allow ECS application services to access Redis"
}


# =========================================================
# Observability
# =========================================================

resource "aws_security_group" "observability" {
  name        = "clothflow-observability-sg"
  description = "Security group for ClothFlow observability services"
  vpc_id      = aws_vpc.this.id

  tags = {
    Name    = "clothflow-observability-sg"
    Project = "clothflow"
  }
}

# Prometheus/Grafana -> application services
resource "aws_vpc_security_group_ingress_rule" "app_from_observability_product" {
  security_group_id            = aws_security_group.app.id
  referenced_security_group_id = aws_security_group.observability.id

  from_port   = 8080
  to_port     = 8080
  ip_protocol = "tcp"

  description = "Prometheus access to Product Service"
}

resource "aws_vpc_security_group_ingress_rule" "app_from_observability_user" {
  security_group_id            = aws_security_group.app.id
  referenced_security_group_id = aws_security_group.observability.id

  from_port   = 8081
  to_port     = 8081
  ip_protocol = "tcp"

  description = "Prometheus access to User Service"
}

resource "aws_vpc_security_group_ingress_rule" "app_from_observability_inventory" {
  security_group_id            = aws_security_group.app.id
  referenced_security_group_id = aws_security_group.observability.id

  from_port   = 8082
  to_port     = 8082
  ip_protocol = "tcp"

  description = "Prometheus access to Inventory Service"
}

resource "aws_vpc_security_group_ingress_rule" "app_from_observability_order" {
  security_group_id            = aws_security_group.app.id
  referenced_security_group_id = aws_security_group.observability.id

  from_port   = 8083
  to_port     = 8083
  ip_protocol = "tcp"

  description = "Prometheus access to Order Service"
}

resource "aws_vpc_security_group_ingress_rule" "app_from_observability_payment" {
  security_group_id            = aws_security_group.app.id
  referenced_security_group_id = aws_security_group.observability.id

  from_port   = 8084
  to_port     = 8084
  ip_protocol = "tcp"

  description = "Prometheus access to Payment Service"
}

resource "aws_vpc_security_group_ingress_rule" "app_from_observability_shipping" {
  security_group_id            = aws_security_group.app.id
  referenced_security_group_id = aws_security_group.observability.id

  from_port   = 8085
  to_port     = 8085
  ip_protocol = "tcp"

  description = "Prometheus access to Shipping Service"
}

resource "aws_vpc_security_group_ingress_rule" "app_from_observability_notification" {
  security_group_id            = aws_security_group.app.id
  referenced_security_group_id = aws_security_group.observability.id

  from_port   = 8086
  to_port     = 8086
  ip_protocol = "tcp"

  description = "Prometheus access to Notification Service"
}

# Grafana -> Prometheus
resource "aws_vpc_security_group_ingress_rule" "observability_from_observability_prometheus" {
  security_group_id            = aws_security_group.observability.id
  referenced_security_group_id = aws_security_group.observability.id

  from_port   = 9090
  to_port     = 9090
  ip_protocol = "tcp"

  description = "Observability services access Prometheus"
}

resource "aws_security_group" "efs" {
  name        = "clothflow-efs-sg"
  description = "Security group for ClothFlow EFS"
  vpc_id      = aws_vpc.this.id

  tags = {
    Name        = "clothflow-efs-sg"
    Project     = "clothflow"
    Environment = "dev"
  }
}

resource "aws_vpc_security_group_ingress_rule" "efs_from_observability" {
  security_group_id            = aws_security_group.efs.id
  referenced_security_group_id = aws_security_group.observability.id

  ip_protocol = "tcp"
  from_port   = 2049
  to_port     = 2049
}

resource "aws_vpc_security_group_egress_rule" "efs_all" {
  security_group_id = aws_security_group.efs.id

  cidr_ipv4   = "0.0.0.0/0"
  ip_protocol = "-1"
}

resource "aws_vpc_security_group_ingress_rule" "grafana_from_alb" {
  security_group_id            = aws_security_group.observability.id
  referenced_security_group_id = aws_security_group.alb.id

  ip_protocol = "tcp"
  from_port   = 3000
  to_port     = 3000
}