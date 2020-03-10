package serviceImpl;

import common.Const;
import common.ServerResponse;
import dao.CustomerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pojo.Customer;
import pojo.Email;
import service.CustomerService;
import until.EmailUntil;

import java.util.List;

import javax.mail.MessagingException;

@Service
public class CustomerServiceImpl implements CustomerService {

    @Autowired
    private CustomerMapper customerMapper;
    private EmailUntil emailUntil = new EmailUntil();

    public ServerResponse findAllCustomer() {
        List<Customer> customerList= customerMapper.findAllCustomer();
        return ServerResponse.createBySuccess(customerList);
    }


    public ServerResponse findCustomerById(Integer id) {
        return ServerResponse.createBySuccess(customerMapper.findCustomerById(id));
    }

    //获取客户总数量
    public ServerResponse findAllCustomersQuantity() {
        
        return ServerResponse.createBySuccess(customerMapper.findCustomersQuantity());
    }

    //给订阅的用户发送邮件
    public ServerResponse postMailToMarkingCustomer() throws MessagingException {
        List<String> emailList = customerMapper.findEmailByMark(Const.CustomerMark.MARK);
        if (emailList != null){
            emailUntil.emailPost(emailList);
        }
        return ServerResponse.createBySuccess();
    }

    //存储客户信息
    public ServerResponse insertCustomer(Customer customer) throws MessagingException {
        //先查询该邮箱是否已经存在数据库
        if (customerMapper.findCustomersByEmail(customer.getEmail()).size() != 0){
            //如果存在，把数据库内该邮箱的记录的订阅状态更新一下
            customerMapper.updateMarkByEmail(customer);
        }
        //把客户信息存进数据库
    	int status = customerMapper.insertCustomer(customer);
        if(status!=0){
        	//异步把客户信息通过邮件发送给管理员
            Runnable runnable =new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(5000);
                        //把顾客信息通过邮件发给管理员
                        emailUntil.sendCustomerInfo(customer);
                        Email email = new Email();
                        //设置标题
                        email.setSubject("订阅成功");
                        //设置内容
                        email.setContent("感谢你的订阅");
                        //设置收件人
                        email.setRecipient(customer.getEmail());
                        //发送并且获取返回值
                        ServerResponse serverResponse = emailUntil.emailPost(email);
                        if (serverResponse.getStatus() == 0){
                            customerMapper.deleteCustomerByEmail(customer.getEmail());
                            System.out.println("该无效邮箱已经删除");
                        }
                    } catch (MessagingException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            //启动上面的线程
            Thread thread=new Thread(runnable);
            thread.start();
            return ServerResponse.createBySuccess();
        }
        else {
            return ServerResponse.createByError();
        }

    }

    //订阅和取订功能
    public ServerResponse updateMarkByEmail(Customer customer) throws MessagingException {
        int status = 0;
        //通过邮箱查找订阅状态
        Integer mark = customerMapper.findMarkByEmail(customer.getEmail());
        //如果用户的请求是订阅的话
        if(customer.getMark() == Const.CustomerMark.MARK){
            //如果mark不为空，说明数据库存在该用户的信息，直接修改订阅状态就可以
            if(mark != null){
                //如果该用户已经订阅过了
                if (mark == Const.CustomerMark.MARK){
                    return ServerResponse.createByErrorMessage("该邮箱已经订阅过了");
                }
                //否则把用户的mark状态改为订阅
                else {
                    status = customerMapper.updateMarkByEmail(customer);
                }
            }
            //如果数据库中没有该用户信息，则增加一个新用户
            else {
                status= customerMapper.insertCustomer(customer);
            }
            if (status > 0){
                //给用户发一封订阅成功的邮件
                Email email = new Email();
                //设置标题
                email.setSubject("订阅成功");
                //设置内容
                email.setContent("感谢你的订阅");
                //设置收件人
                email.setRecipient(customer.getEmail());
                //发送并且获取返回值
                ServerResponse serverResponse = emailUntil.emailPost(email);
                System.out.println(serverResponse.getStatus());
                //如果发送失败 说明邮箱无效，删除该邮箱的所有记录
                if (serverResponse.getStatus() == 0){
                    customerMapper.deleteCustomerByEmail(customer.getEmail());
                    return ServerResponse.createByErrorMessage("请输入有效的邮箱地址");
                }
                return  ServerResponse.createBySuccessMessage("订阅成功");
            }
        }
        //如果用户的操作是不是订阅的话
        else {
            //检测用户的请求是否为取订请求
            if (customer.getMark() == Const.CustomerMark.NO_MARK){
                //如果数据库中该用户的mark不是已订阅状态的话，返回错误msg该邮箱没有订阅
                if(mark != Const.CustomerMark.MARK){
                    ServerResponse.createBySuccessMessage("该邮箱没有订阅");
                }
                //如果数据库中该用户的mark是已订阅状态，则执行取订操作
                else {
                    status = customerMapper.updateMarkByEmail(customer);
                    if (status>0){
                        ServerResponse.createBySuccess("取订成功");
                    }
                }
            }
        }
        return ServerResponse.createByErrorMessage("操作失败");
    }

    //删除顾客信息
    public ServerResponse deleteCustomerById(Integer id) {
        int status = customerMapper.deleteCustomerById(id);
        if(status != 0){
            return ServerResponse.createBySuccess();
        }
        else{
            return ServerResponse.createByError();
        }
    }
}