
> **文档状态：远期架构参考，非当前实施方案。**  
> 当前 MVP 已确定使用 Java 21 + Spring Boot DDD 模块化单体，并通过 Docker Compose 部署。开发请以 [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md) 和 [OpenAPI 契约](docs/openapi.yaml) 为准；下文的微服务、EKS、Kafka 与 Kubernetes 仅保留为未来规模化讨论材料。

下面按照一个**真实可商业化 ToC 平台（CreatorHub）**的标准，设计完整技术架构。

目标：

* 支撑百万级用户
* 支撑千万级作品
* 支撑图片/视频大量上传
* 支撑交易支付
* 支撑社交关系
* 支撑搜索推荐
* 支撑未来 AI 能力扩展

整体采用：

> **Cloud Native + Kubernetes + Microservices + Event Driven Architecture**

---

# 一、整体技术架构图

## 1. 全链路架构

```text
                         User

          ┌────────────────────────┐
          │                        │
       Web Browser              Mobile App
          │                        │
          └───────────┬────────────┘
                      │

                CDN / Edge Network
                      │

              AWS CloudFront
                      │

              API Gateway Layer
                      │

        ┌─────────────┴─────────────┐
        │                           │

   Authentication              Load Balancer
   Service                     (ALB)


                      │

              Kubernetes Cluster

                 Amazon EKS


                      │

 ==================================================

                    Backend Services


 ┌────────────┐       ┌─────────────┐
 │ User       │       │ Creator     │
 │ Service    │       │ Service     │
 └────────────┘       └─────────────┘


 ┌────────────┐       ┌─────────────┐
 │ Artwork    │       │ Marketplace │
 │ Service    │       │ Service     │
 └────────────┘       └─────────────┘


 ┌────────────┐       ┌─────────────┐
 │ Order      │       │ Payment     │
 │ Service    │       │ Service     │
 └────────────┘       └─────────────┘


 ┌────────────┐       ┌─────────────┐
 │ Social     │       │ Notification│
 │ Service    │       │ Service     │
 └────────────┘       └─────────────┘


 ┌────────────┐       ┌─────────────┐
 │ Search     │       │ Recommend  │
 │ Service    │       │ Service    │
 └────────────┘       └─────────────┘


 ==================================================


                 Data Layer


 PostgreSQL
 Redis
 Elasticsearch
 S3
 Kafka


                 Monitoring


 CloudWatch
 Prometheus
 Grafana

```

---

# 二、整体技术分层

分为 6 层：

```
┌───────────────────────────┐
│        Client Layer       │
│ Web / iOS / Android       │
└──────────────┬────────────┘


┌──────────────▼────────────┐
│       Gateway Layer       │
│ API Gateway               │
│ Authentication            │
│ Rate Limit                │
└──────────────┬────────────┘


┌──────────────▼────────────┐
│      Business Layer       │
│ Microservices             │
└──────────────┬────────────┘


┌──────────────▼────────────┐
│       Event Layer         │
│ Kafka / SQS               │
└──────────────┬────────────┘


┌──────────────▼────────────┐
│        Data Layer         │
│ DB / Cache / Storage      │
└──────────────┬────────────┘


┌──────────────▼────────────┐
│ Infrastructure Layer      │
│ AWS / Kubernetes          │
└───────────────────────────┘

```

---

# 三、微服务拆分设计

## 1. User Service 用户服务

职责：

管理用户身份。

功能：

* 注册
* 登录
* OAuth
* 用户资料
* 权限

数据库：

User DB

接口：

```
POST /users/register

POST /users/login

GET /users/{id}

PUT /users/profile

```

---

# 2. Creator Service 创作者服务

核心服务。

负责：

* Creator Profile
* 认证
* 粉丝
* 创作者等级

数据：

Creator

例如：

```
Creator

id

user_id

category

followers

verification

rating

```

---

接口：

```
POST /creator/apply


GET /creator/{id}


POST /creator/follow

```

---

# 3. Artwork Service 作品服务

核心商品系统。

负责：

作品：

* 创建
* 修改
* 发布
* 下架
* 库存

数据库：

Artwork DB

结构：

```
Artwork

id

creator_id

title

description

price

stock

status

```

---

接口：

```
POST /artworks


GET /artworks/feed


GET /artworks/{id}


DELETE /artworks/{id}

```

---

# 4. Media Service 媒体服务

负责：

图片：

视频：

文件。

架构：

```
Client

 |

Upload API

 |

S3 Bucket

 |

CloudFront CDN

```

流程：

用户上传图片：

↓

获取 Upload URL

↓

直接上传 S3

↓

后台生成：

* 缩略图
* 压缩
* 水印

技术：

AWS S3

Lambda

CloudFront

---

# 5. Search Service 搜索服务

负责：

作品搜索。

技术：

Elasticsearch

支持：

关键词：

```
陶瓷

摄影

日本

手工

```

过滤：

价格

类别

作者

---

索引：

```
Artwork Index


{
title,

tags,

creator,

price,

category

}

```

---

# 6. Social Service 社交服务

负责：

关系。

功能：

* Follow
* Like
* Comment
* Save

数据库：

Social DB

例如：

Follow:

```
user_id

creator_id

created_time

```

---

# 7. Order Service 订单服务

负责：

交易。

状态机：

```
Created

 ↓

Paid

 ↓

Processing

 ↓

Shipping

 ↓

Completed

```

---

数据库：

Order

---

接口：

```
POST /orders


GET /orders/{id}


PUT /orders/status

```

---

# 8. Payment Service 支付服务

第三方：

Stripe

PayPal

流程：

```
Order


 |

Payment Service


 |

Stripe


 |

Webhook


 |

Order Update

```

---

# 9. Notification Service

通知：

* 新作品
* Drop
* 订单
* 评论

支持：

Push

Email

SMS

技术：

AWS SNS

---

# 10. Recommendation Service 推荐系统

第一阶段：

规则推荐。

例如：

```
用户喜欢摄影

↓

推荐摄影作品

```

第二阶段：

机器学习。

数据：

用户行为：

* 点击
* 收藏
* 浏览
* 购买

模型：

Two Tower

Embedding

---

# 四、核心数据架构

## PostgreSQL

业务数据：

```
User

Creator

Artwork

Order

Payment

```

---

## Redis

缓存：

热门作品：

```
hot_artworks

```

Session：

```
session:user_id

```

排行榜：

```
creator_rank

```

---

## Elasticsearch

搜索：

```
Artwork

Creator

Tags

```

---

## S3

存储：

```
/images

/videos

/creator-assets

```

---

## Kafka

事件流：

```
ArtworkCreated


        |

        +--> Search Update


        +--> Recommendation


        +--> Notification


        +--> Analytics

```

---

# 五、AWS 云部署方案

## Region

建议：

美国：

us-east-1

或者

全球：

Multi Region

---

# 网络架构

```
AWS VPC


 ├── Public Subnet

 │

 │    Load Balancer


 └── Private Subnet


      EKS Cluster


      Database


      Cache


```

---

# Kubernetes 架构

Amazon EKS

节点：

```
Node Group


 ├── API Pods

 ├── Service Pods

 ├── Worker Pods

 └── ML Pods

```

---

# 六、Kubernetes 部署

## Namespace

```
creatorhub-prod


├── frontend

├── backend

├── database

├── monitoring

```

---

## Service Deployment

例如：

Artwork Service

Deployment:

```
replicas:3


container:

artwork-service:v1

```

---

Service:

```
ClusterIP


artwork-service

```

---

# 七、CI/CD 流程

代码：

GitHub

↓

GitHub Actions

↓

Docker Build

↓

Amazon ECR

↓

Kubernetes Deploy

↓

Production

流程：

```
Developer

 |

Git Push

 |

CI Test

 |

Docker Image

 |

ECR

 |

Helm Deploy

 |

EKS

```

---

# 八、监控体系

## Metrics

Prometheus

采集：

* CPU
* Memory
* API latency
* Error rate

---

## Dashboard

Grafana

监控：

```
DAU

GMV

Orders

Latency

Errors

```

---

## Logging

ELK:

```
ElasticSearch

Logstash

Kibana

```

---

# 九、高并发设计

## 图片访问

不要：

用户 → 后端 → 图片

应该：

用户

↓

CloudFront

↓

S3

---

## 热门作品

Redis:

```
Top Artwork Cache


```

---

## 秒杀 Drop

例如：

限量作品：

100 件

架构：

```
Request


 |

Queue


 |

Order Worker


 |

Inventory DB

```

避免库存超卖。

---

# 十、安全设计

## 用户认证

JWT

Access Token:

15min

Refresh Token:

30 days

---

## 数据安全

* HTTPS
* Encryption at Rest
* Database backup

---

## 支付安全

不保存银行卡。

全部：

Stripe Token

---

# 十一、未来 AI 扩展接口

虽然不是 AI 产品，但未来可以加：

## AI Creator Assistant

服务：

```
AI Service


|

LLM API


|

Creator Studio

```

能力：

* 自动生成商品描述
* 推荐价格
* 图片优化
* SEO

---

## AI Recommendation

数据：

```
User Behavior


↓

Embedding


↓

Vector DB


↓

Recommendation

```

技术：

Pinecone

Milvus

OpenSearch Vector

---

# 十二、开发团队配置

MVP：

## 5 人团队

### Backend

2人

Node/NestJS

### Frontend

1人

React

### Mobile

1人

Flutter

### DevOps

兼职

---

成熟阶段：

15-20人：

Backend:

8

Frontend:

3

Mobile:

2

Data:

2

DevOps:

2

---

# 十三、成本预估

## MVP阶段

用户：

1万以内

AWS:

约：

$300-$1000/月

---

## 成长期

用户：

100万

AWS:

$20k-$50k/月

主要成本：

* CDN
* 图片存储
* 数据库
* 搜索

---

# 十四、最终演进路线

```
Phase 1

Marketplace

作品交易


↓

Phase 2

Creator Economy

粉丝 + 社区


↓

Phase 3

Creator SaaS

创作者工具


↓

Phase 4

Global Creator Network

全球创作者经济基础设施

```

---