# frontend

This template should help get you started developing with Vue 3 in Vite.

---

## Important! How to contribute to this project

- step1: check out a new branch:

```sh
git checkout -b 'your_branch_name'
```

- step2: write your components in directories `src/views` or `src/components`

  > It is recommended that the `view` directory contains the first-level components that router routes, while the `components` directory contains components that your `view/*` components imports.
  >
- optional step3: if necessary, modify `src/router/index.js` to add your view to a new route.
- step4: commit, push to your own branch, issue a PR to merge to branch master

---

## Topology

Please refer to `documents/topology.drawio` to check for latest updates.

Top component is `src/App.vue`, responsible for top and left navigation bar / drawer. It imports `router view`.

The next components is registered using vue router. Depending on the url that access the system, the components imported will be in the range of the following:

- [ / ] `views/Home.vue`
- [ /dev ] `views/Dev.vue`
- [ /plan ] `views/Plan.vue`

---

## Customize configuration

See [Vite Configuration Reference](https://vitejs.dev/config/).

## 项目部署

从 `github.com/l5h71/JiHub-Env` 仓库克隆得到前后端的dockerfile，自行构建docker image

注意：docker 中 package.json 文件和前端项目中 package.json 文件不一致，如果需要安装其他包，或者修改版本。请在群中联系lsh，或者在 github 中发起 issue

### 通过 docker-compose 启动环境

```sh
docker-compose up
```

docker-compose 仅将 `./src`，`./public` 和首页代码 `index.html` 挂载到 docker 中

### 启动 Vue 服务

默认情况下，进入docker会自动运行服务，但如果没有运行，可以通过以下命令进行测试

进入 docker 中运行

```sh
npm run dev -- --port 3000 --host
```

网页将在 `localhost:80` 打开（如果在服务器中，注意开放端口）
