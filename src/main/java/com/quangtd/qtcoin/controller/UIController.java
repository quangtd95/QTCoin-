package com.quangtd.qtcoin.controller;

import com.quangtd.qtcoin.domain.Block;
import com.quangtd.qtcoin.domain.Response;
import com.quangtd.qtcoin.domain.Wallet;
import com.quangtd.qtcoin.form.LoginForm;
import com.quangtd.qtcoin.form.SendCoinForm;
import com.quangtd.qtcoin.service.MainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
public class UIController {
    @Autowired
    private MainService mainService;

    @GetMapping("/")
    public String index(Model model, HttpSession session) {
        if (session.getAttribute("my_address") == null) {
            return "redirect:/login";
        }
        List<Wallet> wallets = mainService.getWallets();
        model.addAttribute("wallets", wallets);
        model.addAttribute("my_address", session.getAttribute("my_address"));
        return "wallet_list";
    }

    @GetMapping("/login")
    public String getLogin(Model model) {
        LoginForm loginForm = new LoginForm();
        model.addAttribute("loginForm", loginForm);
        return "login";
    }

    @PostMapping("/login")
    public String postLogin(Model model, LoginForm loginForm, HttpSession httpSession) {
        boolean success = mainService.vaildateWallet(loginForm.getAddress(), loginForm.getPrivateKey());
        if (success) {
            httpSession.setAttribute("my_address", loginForm.getAddress());
            return "redirect:/";
        } else {
            model.addAttribute("message", "invalid information!");
            model.addAttribute("loginForm", loginForm);
        }
        return "login";
    }

    @PostMapping("/logout")
    public String postLogin(HttpSession httpSession) {
        httpSession.setAttribute("my_address", null);
        return "redirect:/login";
    }

    @GetMapping("/mineBlock")
    public String mineBLock(Model model, HttpSession session) {
        if (session.getAttribute("my_address") == null) {
            return "redirect:/login";
        }
        Block newBlock = mainService.mineBlockWithAddress((String) session.getAttribute("my_address"));
        if (newBlock != null) {
            return "redirect:/";
        } else {
            model.addAttribute("message", "error");
            return "list";
        }
    }

    @GetMapping("/sendCoins")
    public String getSendCoins(Model model) {
        model.addAttribute("sendCoinForm", new SendCoinForm());
        return "send_coins";
    }

    @PostMapping("/sendCoins")
    public String sendCoins(Model model, SendCoinForm sendCoinForm, HttpSession session) {
        if (session.getAttribute("my_address") == null) {
            return "redirect:/login";
        }
        String myAddress = (String) session.getAttribute("my_address");
        boolean checkMyAddressInformation = mainService.vaildateWallet(myAddress, sendCoinForm.getPrivateKey());
        if (!checkMyAddressInformation) {
            model.addAttribute("message", "error private key");
            return "redirect:/";
        }
        try {
            mainService.sendCoin(sendCoinForm.getPrivateKey(), sendCoinForm.getReceiveAddress(), sendCoinForm.getAmount());
            model.addAttribute("message", "your transaction has been added to transaction pool!");
        } catch (Exception e) {
            model.addAttribute("message", e.getMessage());
        }
        return "redirect:/";

    }


}
