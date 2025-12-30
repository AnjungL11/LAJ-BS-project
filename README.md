# B/S体系软件设计大程 图片管理网站

## 在根目录下运行安装命令
```powershell
npm install @modelcontextprotocol/sdk axios
```
## Docker运行步骤
拉取Node镜像
```powershell
docker pull node:18
``` 
拉取Maven镜像
```powershell
docker pull maven:3.8.5-openjdk-17
```
拉取Java运行环境镜像
```powershell
docker pull eclipse-temurin:17-jre
```
运行Docer Compose
```powershell
docker-compose build --no-cache
docker-compose up -d
```
导入数据库，执行如下命令（相应地数据库文件路径、用户名、密码等需要替换）
```powershell
type C:\Users\李安璟\Desktop\LAJ-BS-project\database\init.sql | docker exec -i image-manager-db mysql -uroot -p123456 image_manager
```
导入数据库后重启后端容器
```powershell
docker restart image-manager-app
```
打开浏览器访问<http://localhost:8080>。