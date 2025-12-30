# 构建Vue前端
FROM node:18 AS frontend-build
WORKDIR /app/frontend

# 复制package.json
COPY frontend/package*.json ./

# 安装项目依赖
RUN npm install --registry=https://registry.npmmirror.com

# 安装vite
RUN npm install -g vite --registry=https://registry.npmmirror.com

# 复制源码
COPY frontend/ .

# 打包
RUN vite build

# 构建Spring Boot后端
FROM maven:3.8.5-openjdk-17 AS backend-build
WORKDIR /app/backend

# 复制Maven配置和源码
COPY backend/pom.xml .
COPY backend/src ./src

# 复制前端产物到后端静态目录
COPY --from=frontend-build /app/frontend/dist ./src/main/resources/static/

# 打包后端
RUN mvn clean package -DskipTests -B -Dmaven.repo.local=/root/.m2/repository

# 运行时环境
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=backend-build /app/backend/target/*.jar app.jar

EXPOSE 8080
VOLUME /app/uploads

ENTRYPOINT ["java", "-jar", "app.jar"]