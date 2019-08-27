package cn.akira.controller;

import cn.akira.pojo.User;
import cn.akira.pojo.UserInfo;
import cn.akira.service.UserService;
import cn.akira.util.CastUtil;
import cn.akira.returnable.CommonData;
import cn.akira.returnable.LayuiTableData;
import cn.akira.util.ImgResizeUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
@Controller
@RequestMapping("/user/")
public class UserController {

    private static final String RESOURCE_PATH = "D:\\Common\\AppData\\Tomcat\\deployed_resources\\akira\\";

    @Autowired
    private UserService userService;

    @RequestMapping("login")
    public String loginPage() {
        return "login";
    }

    @RequestMapping("doLogin")
    @ResponseBody
    public CommonData doLogin(User user, HttpSession session, HttpServletRequest request) throws Exception {
        session.removeAttribute("SESSION_USER");
        boolean unameIsEmpty = user.getUname() == null || user.getUname().equals("");
        boolean phoneIsEmpty = user.getBindPhone() == null || user.getBindPhone().equals("");
        boolean emailIsEmpty = user.getBindEmail() == null || user.getBindEmail().equals("");
        boolean passwordIsEmpty = user.getPassword() == null || user.getPassword().equals("");

        //如果除密码外的三个用作登录凭据的属性都为空 或者 密码为空  均不能登录  懂我意思吧？
        if (unameIsEmpty && phoneIsEmpty && emailIsEmpty || passwordIsEmpty) {
            return new CommonData("缺少关键的登录凭据", false);
        }
        //数据库检查
        String sha1HexPassword = DigestUtils.sha1Hex(user.getPassword()); //用户密码加密
        user.setPassword(sha1HexPassword);
        User dbUser = userService.getUser(user);
        if (dbUser != null) {
            //将用户信息存储到会话的中
            dbUser.setPassword(null);
            session.setAttribute("SESSION_USER", dbUser);
            CommonData result = new CommonData();
            result.setResource(request.getContextPath() + "/index");
            return result;
        } else {
            System.out.println("用户名或密码不正确");
            CommonData result = new CommonData();
            result.setResource(request.getContextPath() + "/user/login");
            result.setFlag(false);
            return result;
        }
    }

    @RequestMapping("userList1")
    public String userListPage1(Model model) throws Exception {
        List<User> userBaseInfoList = userService.getUserBaseInfoList();
        List<User> users = new ArrayList<>();
        for (User user : userBaseInfoList) {
            users.add(CastUtil.genderCast(user));
        }
        model.addAttribute("userList", users);
        return "user/userList1";
    }

    @RequestMapping("userList2")
    public String userListPage2() {
        return "user/userList2";
    }

    @RequestMapping("userList3")
    public String userListPage3() {
        return "user/userList3";
    }

    @RequestMapping("listUser2")
    @ResponseBody
    public CommonData listUser2() {
        try {
            List<User> userBaseInfoList = userService.getUserBaseInfoList();
            List<User> users = new ArrayList<>();
            for (User user : userBaseInfoList) {
                users.add(CastUtil.genderCast(user));
            }
            CommonData commonData = new CommonData();
            commonData.setResource(users);
            return commonData;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @RequestMapping("listUser3")
    @ResponseBody
    public LayuiTableData listUser3() {
        try {
            List<User> userBaseInfoList = userService.getUserBaseInfoList();
            List<User> users = new ArrayList<>();
            for (User user : userBaseInfoList) {
                users.add(CastUtil.genderCast(user));
            }
            return new LayuiTableData(0, users.size(), users);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @RequestMapping("showAddUser")
    public String toAddUserPage() {
        return "user/addUser";
    }

    @RequestMapping("headIconUpload")
    @ResponseBody
    public CommonData uploadUserIcon(@RequestParam("file") MultipartFile multipartFile) {
        String uploadFilePath = RESOURCE_PATH + "image\\head\\cache\\";
        try {
            File cacheDir = new File(uploadFilePath);
            if (!cacheDir.exists() && !cacheDir.mkdir()) {
                return new CommonData("文件上传失败,因为不能创建缓存目录", false);
            }
            System.out.println("缓存路径:\n" + uploadFilePath);
            if (multipartFile.isEmpty()) {
                return new CommonData("请不要试图上传一个空文件", false);
            }
            //获取原始文件名
            String originalFilename = multipartFile.getOriginalFilename();
            if (originalFilename == null) {
                return new CommonData("没有文件名?你是怎么办到的?", false);
            }
            System.out.println("原始文件名 - " + originalFilename);

            //新文件名前缀 = 当前时间戳
            String newPrefixFileName = String.valueOf(new Date().getTime());

            //新文件名 = 新文件名前缀 + 原始文件名后缀(文件格式)
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFileName = newPrefixFileName + suffix;

            String fullPath = uploadFilePath + newFileName; //文件全路径
            //文件名去重
            for (int i = 1; ; i++) {
                if (new File(fullPath).exists()) {
                    newFileName += i;
                } else {
                    break;
                }
            }
            //构建文件流对象
            File uploadFile = new File(fullPath);

            //上传至缓存目录
            multipartFile.transferTo(uploadFile);
            //图片宽高自适应处理
            ImgResizeUtil.selfAdapt(uploadFile);

            CommonData data = new CommonData();
            data.setResource(newFileName);
            data.setMessage("文件上传成功");
            return data;
        } catch (Exception e) {
            System.err.println("文件上传失败");
            e.printStackTrace();
            return new CommonData("文件上传失败了", e);
        }
    }

    @RequestMapping("createUser")
    @ResponseBody
    public CommonData createUser(
            @ModelAttribute User user,
            @RequestParam("rePassword") String rePassword) {


        String emailReg = "^[a-z0-9A-Z]+[-|a-z0-9A-Z._]+@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-z]{2,}$";
        String chineseMainLandPhoneReg = "^1([38][0-9]|4[579]|5[0-3,5-9]|6[6]|7[0135678]|9[89])\\d{8}$";
        String uname = user.getUname();
        String bindPhone = user.getBindPhone();
        String bindEmail = user.getBindEmail();
        String password = user.getPassword();
        String email = user.getUserInfo().getEmail();
        String addr = user.getUserInfo().getAddr();
        String headIcon = user.getUserInfo().getHeadIcon();
        String realName = user.getRealNameAuth().getRealName();
        String cid = user.getRealNameAuth().getCid();
        String certType = user.getRealNameAuth().getCertType();
        String iconCachePath = RESOURCE_PATH + "image\\head\\cache\\";
        String iconCacheFileFullPath = iconCachePath + headIcon;
        if (uname == null) {
            return new CommonData("用户名是必填的哦~", "uname", false);
        } else if (uname.length() < 3) {
            return new CommonData("用户名至少3个字符", "uname", false);
        } else if (uname.contains(" ") || uname.contains("@")) {
            return new CommonData("用户名不能有空格或者艾特符", "uname", false);
        } else if (uname.matches(chineseMainLandPhoneReg)) {
            return new CommonData("不要用疑似手机号格式的用户名嘛", "uname", false);
        }

        if (bindPhone == null && bindEmail == null) {
            return new CommonData("邮箱和手机至少得绑一个吧", false);
        }

        if (bindPhone != null && !bindPhone.matches(chineseMainLandPhoneReg)) {
            return new CommonData("手机号格式不对", "bindPhone", false);
        }

        if (bindEmail != null && !bindEmail.matches(emailReg)) {
            return new CommonData("邮箱格式没写对", "bindEmail", false);
        }

        // 密码校验
        if (password.replace(" ", "").equals("")) {
            return new CommonData("密码没输", "password", false);
        } else if (password.contains(" ")) {
            return new CommonData("你不能设置有空格的密码", "password", false);
        } else if (!password.matches("^(?![0-9]+$)(?![a-zA-Z]+$)[0-9A-Za-z]{6,20}$")) {
            return new CommonData("密码要有英文字母和数字，而且长度至少是6位，最多20位", "password", false);
        } else if (!password.equals(rePassword)) {
            return new CommonData("两次输入的密码都不一样,你要搞哪样嘛", "rePassword", false);
        }

        if (email != null && !email.matches(emailReg)) {
            return new CommonData("邮箱格式不正确", "email", false);
        }
        if (addr != null && (addr.replace(" ", "").length() < 5)) {
            return new CommonData("地址写详细点嘛", "addr", false);
        }
        if (!((/*都为空*/
                realName == null &&
                        cid == null &&
                        certType == null
        ) || (/*都不为空*/
                realName != null &&
                        cid != null &&
                        certType != null
        ))) {
            return new CommonData("如需实名信息,就请将其完善", false);
        } else if (cid != null && certType.equals("1") && !cid.matches("^[1-9]\\d{7}((0\\d)|(1[0-2]))(([0|1|2]\\d)|3[0-1])\\d{3}$|^[1-9]\\d{5}[1-9]\\d{3}((0\\d)|(1[0-2]))(([0|1|2]\\d)|3[0-1])\\d{3}([0-9]|X|x)$")) {
            return new CommonData("身份证格式不对", "cid", false);
        }

        try {
            //sha1校验，将上传的头像存入目标路径
            File iconCacheFile = new File(iconCacheFileFullPath);
            FileInputStream fileInputStream = new FileInputStream(iconCacheFile);
            String suffix = headIcon.substring(headIcon.lastIndexOf("."));
            String hexedFileName = DigestUtils.sha1Hex(fileInputStream) + suffix;
            fileInputStream.close();
            String hexedFilePath = iconCachePath + "..\\" + hexedFileName;
            File hexedFileNamePath = new File(hexedFilePath);
            if (!hexedFileNamePath.exists()) {
                Files.move(iconCacheFile.toPath(), hexedFileNamePath.toPath());
            }
            UserInfo userInfo = user.getUserInfo() == null ? new UserInfo() : user.getUserInfo();
            userInfo.setHeadIcon(hexedFileName);
            user.setUserInfo(userInfo);
            user.setPassword(DigestUtils.sha1Hex(user.getPassword()));
            return userService.createUser(user);
        } catch (Exception e) {
            e.printStackTrace();
            return new CommonData("创建用户时遇到一个错误", e);
        }
    }

    @RequestMapping("deleteUser")
    @ResponseBody
    public CommonData deleteUsers(@RequestParam("ids[]") List<Integer> ids) {
        try {
            return userService.deleteUsers(ids);
        } catch (Exception e) {
            return new CommonData("批量删除失败了", false);
        }
    }

    @RequestMapping("checkUsername")
    @ResponseBody
    public CommonData checkUsername(@RequestParam("uname") String uname) {
        try {
            return userService.getUserByUname(uname);
        } catch (Exception e) {
            e.printStackTrace();
            return new CommonData("检查用户名占用时发生异常", e);
        }
    }

    @RequestMapping("exit")
    public String exit(HttpSession session) {
        session.removeAttribute("SESSION_USER");
        return "redirect:/";
    }
}