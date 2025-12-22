# FRONTEND
## 前端运行
先运行以下命令进入 `frontend` 目录。
```powershell
cd frontend
```
安装相关依赖。
```powershell
npm install
npm install cropperjs@1.6.2
```
运行前端。
```powershell
npm run dev
```

## PC端
打开电脑浏览器，进入 <http://localhost:3000> 即可看到初始界面。

## 手机端
在电脑cmd命令行输入如下命令找到WLAN下对应的IPv4地址，以我的电脑为例，地址为 `10.162.133.194`。
```powershell
ipconfig
```
然后再手机端打开浏览器输入网址 <http://10.162.133.194:3000> 即可在手机端打开进入该系统。

注意确保手机和电脑处于同一WLAN下。