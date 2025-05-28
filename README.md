[![警报](http://8.140.28.219:9000/api/project_badges/measure?project=jihub&metric=alert_status&token=sqb_e0b1817c4f9c2b67aeab1b356a68c827248ea2f9)](http://8.140.28.219:9000/dashboard?id=jihub)

# BACKEND

注意：后端项目必须在 Linux 环境下运行。

在Github中缺失 `myApp/codeTrans`部分内容，请自行拷贝

## requirements

python 依赖文件已通过 pipreqs 自动生成

## 使用 docker 镜像

从 `github.com/l5h71/JiHub-Env` 仓库克隆得到前后端的dockerfile，自行构建docker image

注意：docker 中 requirements.txt 文件和前端项目中 requirements.txt 文件不一致，如果需要安装其他包，或者修改版本。请在群中联系lsh，或者在 github 中发起 issue

### 通过 docker-compose 启动环境

```sh
docker-compose up
```

docker-compose 仅将 `./djangoProject`，`./myApp`，`manage.py`，`rebuildsqlite.sh` 挂载到 docker 中

后端将在 `localhost:8000` 打开（如果在服务器中，注意开放端口）

## 运行后端项目：

重新建立数据库：

```bash
source rebuildsqlist.sh
```

项目根目录下执行：

```bash
python manage.py runserver 0.0.0.0:8000
```

## 更新数据库表

如果更改了数据库表，需要在项目根目录下执行：

```bash
python manage.py makemigrations
python manage.py migrate
```

如果数据库提示表已存在或字段已存在错误，请删库并重新建库。

## 数据库 rebuild

目前自定义了 rebuilddb 命令来重置数据库信息，在项目根目录下执行：

```bash
python manage.py rebuilddb
```

时，会先删除数据库所有表中的条目，将所有的自增 id 重置为 1，然后向数据库表中插入一些初始化条目，如 10 个 用户和 10 个项目。

可以自行修改 `myApp/management/commands/rebuilddb.py` 中 `buildDataBase` 函数来初始化数据库信息。

## api 开发

- 在 myApp/<你负责的模块>.py 中添加视图函数
- 在 djangoProject/urls.py 中添加视图函数 url
- 在 apifox中撰写接口文档
- 在 apifox中进行接口测试
- settings.py 中提供了调试打印函数 `DBG`
