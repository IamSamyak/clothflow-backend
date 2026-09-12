data "aws_iam_policy_document" "github_actions_deploy" {

  statement {
    sid    = "EcrAuthentication"
    effect = "Allow"

    actions = [
      "ecr:GetAuthorizationToken"
    ]

    resources = ["*"]
  }

  statement {
    sid    = "EcrPushPull"
    effect = "Allow"

    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:CompleteLayerUpload",
      "ecr:InitiateLayerUpload",
      "ecr:PutImage",
      "ecr:UploadLayerPart",
      "ecr:BatchGetImage",
      "ecr:DescribeImages",
      "ecr:DescribeRepositories"
    ]

    resources = [
      "arn:aws:ecr:us-east-1:000000000000:repository/clothflow/*"
    ]
  }

  statement {
    sid    = "EcsDeployment"
    effect = "Allow"

    actions = [
      "ecs:DescribeServices",
      "ecs:DescribeTaskDefinition",
      "ecs:DescribeTasks",
      "ecs:ListTasks",
      "ecs:RegisterTaskDefinition",
      "ecs:UpdateService"
    ]

    resources = ["*"]
  }

  statement {
    sid    = "EcsRead"
    effect = "Allow"

    actions = [
      "ecs:ListClusters",
      "ecs:ListServices",
      "ecs:ListTaskDefinitions"
    ]

    resources = ["*"]
  }

  statement {
    sid    = "PassClothFlowEcsRoles"
    effect = "Allow"

    actions = [
      "iam:PassRole"
    ]

    resources = concat(
      [var.ecs_execution_role_arn],
      var.ecs_task_role_arns
    )

    condition {
      test = "StringEquals"

      variable = "iam:PassedToService"

      values = [
        "ecs-tasks.amazonaws.com"
      ]
    }
  }
}

resource "aws_iam_policy" "github_actions_deploy" {
  name = "clothflow-github-actions-deploy"

  policy = data.aws_iam_policy_document.github_actions_deploy.json

  tags = {
    Project     = "clothflow"
    Environment = var.environment
  }
}

resource "aws_iam_role_policy_attachment" "github_actions_deploy" {
  role = aws_iam_role.github_actions.name

  policy_arn = aws_iam_policy.github_actions_deploy.arn
}

