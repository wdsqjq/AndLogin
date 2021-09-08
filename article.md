# AndLogin

1，何时判断拦截
2，如何判断
3，登陆后如何继续原意图





告诉框架几点：
1, 你登录的activty

2, 你哪些activity需要登录

3, 你是怎么判断是否登录的



```
// A more classic application is to handle login events during a jump so that there is no need to repeat the login check on the target page.
```

参考：

https://github.com/Xiasm/LoginArchitecture

https://juejin.cn/post/6844903657817767943

当非登录态时启动登录界面登录成功以后自动帮用户继续之前被打断的操作