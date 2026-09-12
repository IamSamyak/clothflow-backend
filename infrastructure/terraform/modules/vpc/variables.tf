variable "vpc_cidr" {
  description = "CIDR block for the ClothFlow VPC"
  type        = string
}

variable "availability_zones" {
  description = "Availability zones used by ClothFlow"
  type        = list(string)
}