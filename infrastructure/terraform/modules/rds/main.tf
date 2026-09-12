resource "aws_db_subnet_group" "this" {
  name = "clothflow-db-subnet-group"

  subnet_ids = var.db_subnet_ids

  tags = {
    Name    = "clothflow-db-subnet-group"
    Project = "clothflow"
  }
}

resource "aws_db_instance" "this" {
  identifier = "clothflow-db"

  engine         = "postgres"
  engine_version = "16"

  instance_class = "db.t4g.micro"

  allocated_storage = 20
  storage_type      = "gp2"
  storage_encrypted = true

  db_name  = var.db_name
  username = var.db_username

  manage_master_user_password = true

  port = 7001

  db_subnet_group_name   = aws_db_subnet_group.this.name
  vpc_security_group_ids = [var.security_group_id]

  publicly_accessible = false

  multi_az = false

  backup_retention_period = 7

  deletion_protection = false

  skip_final_snapshot = true

  auto_minor_version_upgrade = true

  tags = {
    Name        = "clothflow-db"
    Environment = "dev"
    Project     = "clothflow"
  }
}