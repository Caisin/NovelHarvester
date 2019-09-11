package com.unclezs.Crawl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.unclezs.Model.AnalysisConfig;
import com.unclezs.Model.AudioBook;
import com.unclezs.Model.AudioChapter;
import com.unclezs.Utils.Config;
import com.unclezs.Utils.HtmlUtil;
import com.unclezs.Utils.HttpUtil;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 *@author unclezs.com
 *@date 2019.06.22 00:16
 */
public class AudioNovelSpider {
    private final static Properties conf = new Properties();
    private boolean isPhone = false;
    private String ua = "Mozilla/5.0 (Linux; U; Android 9; zh-CN; MI MAX 3 Build/PKQ1.190118.001) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/57.0.2987.108 UCBrowser/12.5.0.1030 Mobile Safari/537.36";

    static {
        try {//加载配置
            conf.load(AudioNovelSpider.class.getResourceAsStream("/conf/audio.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setPhone(boolean phone) {
        this.isPhone = phone;
    }

    /**
     * 根据名字爬取指定网站搜索结果
     *
     * @param key
     * @param site
     * @return
     */
    public List<AudioBook> searchBook(String key, String site, String keyType) {
        String keyWord = key;//防止编码后无法识别
        List<AudioBook> list = new ArrayList<>();
        try {
            String tmpImg = "";
            if (site.equals("ysts8") || site.equals("ysxs8") || site.equals("audio699") || site.equals("tingchina")) {//无图处理
                tmpImg = new NovelSpider(new AnalysisConfig()).crawlDescImage(key);
                //有声听书吧关键字URL编码
                key = URLEncoder.encode(key, conf.getProperty(site + "_charset"));
            }
            //抓取解析
            String html;
            if (conf.getProperty(site + "_method").equals("get")) {//get
                html = HtmlUtil.getHtml(conf.getProperty(site + "_searchUrl") + key, conf.getProperty(site + "_charset"));
            } else {//post方式
                List<NameValuePair> param = new ArrayList<>();
                param.add(new BasicNameValuePair(conf.getProperty(site + "_searchKey"), key));//请求数据
                html = HttpUtil.doPost(conf.getProperty(site + "_searchUrl"), param, conf.getProperty(site + "_charset"));
            }
            Document document = Jsoup.parse(html);
            document.setBaseUri(conf.getProperty(site + "_searchUrl"));//设置根地址
            Elements lis = document.select(conf.getProperty(site + "_list"));//结果列表
            for (Element li : lis) {
                //标题
                String title;
                Element elementTitle = li.select(conf.getProperty(site + "_title")).first();
                if (site.equals("ting55")||site.equals("ting22")) {//恋听网标题特殊处理
                    title = elementTitle.text();
                } else {
                    title = elementTitle.ownText();
                }
                //图片
                String img = "";
                //作者
                String author = "";
                if (!site.equals("ysts8") && !site.equals("tingchina")) {
                    img = li.select(conf.getProperty(site + "_img")).first().absUrl("src");
                    author = li.select(conf.getProperty(site + "_author").split(",")[0])//获取选择器
                            .get(Integer.parseInt(conf.getProperty(site + "_author").split(",")[1]))//第几个标签
                            .text();
                }
                //播音
                String speak = "";
                if (!site.equals("tingchina")) {
                    speak = li.select(conf.getProperty(site + "_speak").split(",")[0])
                            .get(Integer.parseInt(conf.getProperty(site + "_speak").split(",")[1]))
                            .text();
                }

                //目录地址
                String homeUrl = li.select(conf.getProperty(site + "_url")).first().absUrl("href");

                //特殊处理
                if (site.equals("ting56")) {
                    author = author.split(" ")[0];
                    speak = speak.split(" ")[1];
                }
                if (site.equals("tingchina")) {//听中国的声音
                    author = "未知";
                    img = tmpImg;
                    speak = "未知";
                }
                if (site.equals("ysts8")) {//有声听书吧特殊处理
                    author = "未知";
                    img = tmpImg;
                    speak = speak.split("／")[0];
                }
                if (site.equals("ysxs8") || site.equals("audio699")) {
                    img = tmpImg;
                }
                //后处理
                if (!author.contains("：")) {
                    author = "作者：" + author;
                }
                if (!speak.contains("：")) {
                    speak = "播音：" + speak;
                }
                //根据搜索类型确定结果
                switch (keyType) {
                    case "书名":
                        if (title.contains(keyWord)) {//包含搜索关键字才添加,失效不添加
                            list.add(new AudioBook(author.trim(), speak.trim(), title.trim(), img, homeUrl.trim()));
                        }
                        break;
                    case "作者":
                        if (author.contains(keyWord)) {//包含搜索关键字才添加,失效不添加
                            list.add(new AudioBook(author.trim(), speak.trim(), title.trim(), img, homeUrl.trim()));
                        }
                        break;
                    case "播音":
                        if (speak.contains(keyWord)) {//包含搜索关键字才添加,失效不添加
                            list.add(new AudioBook(author.trim(), speak.trim(), title.trim(), img, homeUrl.trim()));
                        }
                        break;
                    default:
                        list.add(new AudioBook(author.trim(), speak.trim(), title.trim(), img, homeUrl.trim()));
                }

            }
            return list;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return list;
    }

    //获取章节列表
    public List<AudioChapter> getChapters(String url) {
        //懒人听书
        if (url.contains("lrts")) {
            return getLRTSChapters(url);
        }
        //其他
        List<AudioChapter> chapters = new ArrayList<>(100);
        String key = getCase(url);
        String charset = conf.getProperty(key + "_charset");
        String html = HtmlUtil.getHtml(url, charset);
        //解析
        Document document = Jsoup.parse(html);
        document.setBaseUri(url);//根域名
        String[] s = conf.getProperty(key).split(",");//css选择器
        Elements elements = document.select(s[0]).get(Integer.parseInt(s[1])).select(s[2]);//读取配置
        for (Element a : elements) {
            chapters.add(new AudioChapter(a.absUrl("href"), a.text()));
        }
        return chapters;
    }

    //获取有声音频真实地址
    public String getSrc(String url) {
        String realUrl = "";
        switch (getCase(url)) {
            case "ting89":
                realUrl = getTING89(url);
                break;
            case "ysts8":
                realUrl = getYSTS8(url);
                break;
            case "ysxs8":
                realUrl = get520TINGSHU(url);
                break;
            case "ting56":
                realUrl = getTING56(url);
                break;
            case "ting55":
                realUrl = getTING55(url);
                break;
            case "audio699":
                realUrl = getAUDIO699(url);
                break;
            case "520tingshu":
                realUrl = get520TINGSHU(url);
                break;
            case "tingchina":
                realUrl = getTingChina(url);
                break;
            case "lrts":
                realUrl = getLRTS(url);
                break;
            case "ting22":
                realUrl = getTING22(url);
                break;
        }
        try {
            return com.unclezs.Utils.URLEncoder.encode(realUrl, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return realUrl;
    }

    //根据host不同采用不同的方式爬取
    private String getCase(String url) {
        if (url.contains("ting89")) {
            return "ting89";
        } else if (url.contains("ysts8")) {
            return "ysts8";
        } else if (url.contains("ting56")) {
            return "ting56";
        } else if (url.contains("ting55")) {
            return "ting55";
        } else if (url.contains("audio699")) {
            return "audio699";
        } else if (url.contains("520tingshu")) {
            return "520tingshu";
        } else if (url.contains("ysxs8")) {
            return "ysxs8";
        } else if (url.contains("tingchina")) {
            return "tingchina";
        } else if (url.contains("lrts")) {
            return "lrts";
        }else if (url.contains("ting22")) {
            return "ting22";
        } else {
            return "uncle";
        }
    }

    //520听书网和有声小说吧
    private String get520TINGSHU(String curl) {
        String host = curl.substring(0, curl.indexOf("com") + 4);
        String html = HtmlUtil.getSource(curl, "gb2312", curl, null);
        Pattern pattern = Pattern.compile("\"(/playdata/.+?js.*?)\"");
        Matcher m = pattern.matcher(html);
        m.find();
        String jsUrl = host + m.group(1);
        String jsRes = HttpUtil.request(jsUrl);
        String json = jsRes.substring(jsRes.indexOf(',') + 1, jsRes.lastIndexOf("]]"));
        JSONArray array = JSON.parseArray(json);
        String[] indexStr;
        if (curl.contains("520tingshu")) {
            indexStr = curl.replace(".html", "").split("-");
        } else {
            indexStr = curl.replace(".html", "").split("_");
        }
        int index = Integer.parseInt(indexStr[indexStr.length - 1]);
        return array.get(index).toString().split("[$]")[1];
    }

    //幻听网
    private String getTING89(String url) {
        String html = HtmlUtil.getSource(url, "gb2312", url, null);
        String realUrl = Jsoup.parse(html).select("iframe").attr("src");
        realUrl = realUrl.substring(realUrl.lastIndexOf("http"));
        return realUrl;
    }

    //有声听书吧
    private String getYSTS8(String curl) {
        //章节源码爬取
        String chtml = HtmlUtil.getSource(curl, "gbk", curl, null);
        Pattern p = Pattern.compile("<iframe src=\"(.+?)\"");
        Matcher m = p.matcher(chtml);
        m.find();
        //找到真正所在源码网址
        String url = "https://www.ysts8.com" + m.group(1);//真正章节网址
        String prefix = null;
        try {
            prefix = URLDecoder.decode(url.substring(url.indexOf("=") + 1, url.indexOf("&")), "gbk") + "?";
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        //匹配出真实音频url
        String html = HtmlUtil.getHtml(url, "gbk");

        String host = "http://180d.ysts8.com:8000";
        p = Pattern.compile("[?']([0-9a-zA-Z]+?-[0-9a-zA-Z]+?)[?']");
        m = p.matcher(html);
        m.find();
        String realUrl = host + "/" + prefix + m.group(1);//真实URL  m.group(1)加密码
        return realUrl;
    }

    //56听书网
    private String getTING56(String url) {
        String html = HtmlUtil.getSource(url, "gbk", url, null);
        Pattern pattern = Pattern.compile("FonHen_JieMa[(]'([\\s\\S]+?)'");
        Matcher m = pattern.matcher(html);
        m.find();
        String res =getCodeString(m.group(1));
        String realUrl;
        //解密
        String[] datas = res.split("&");//初步数据
        switch (datas[2]) {
            case "tudou":
                String rJson = HttpUtil.request("http://www.ting56.com//player/getcode.php?id=" + datas[0]);
                String code = JSON.parseObject(rJson).get("code").toString();
                if (code != null && "".equals(code)) {
                    realUrl = "http://www.tudou.com/programs/view/html5embed.action?code=" + code + "&autoPlay=true&playType=AUTO";
                } else {
                    realUrl = "http://www.tudou.com/v/" + datas[0];
                }
                break;
            case "tc":
                String[] strings = datas[0].split("/");
                String jmUrl = strings[0] + '/' + strings[1] + "/play_" + strings[1] + "_" + strings[2] + ".htm";
                List<NameValuePair> postData = new ArrayList<>();
                postData.add(new BasicNameValuePair("url", jmUrl));
                String resJson = HttpUtil.doPost("http://www.ting56.com/player/tingchina.php", postData);
                realUrl = JSON.parseObject(resJson).toString().replace("t44", "t33");
                break;
            default:
                realUrl = datas[0].replace(":82", "");
        }
        return realUrl;
    }

    //恋听网
    private String getTING55(String url) {
        String html = HtmlUtil.getSource(url, "utf-8", url, null);
        Pattern p = Pattern.compile("var a=[{].+?\"(.+?)\"");
        Matcher m = p.matcher(html);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }

    //静听网
    private String getAUDIO699(String url) {
        String html = null;
        if (isPhone) {
            url = url.replace("www", "m");
            html = HtmlUtil.getSource(url, "utf-8", url, ua);
        } else {
            html = HtmlUtil.getSource(url, "utf-8", url, null);
        }

        Document document = Jsoup.parse(html);
        String readUrl = document.select("source").attr("src");
        return readUrl;
    }

    //听中国的声音
    private String getTingChina(String url) {
        String chtml = HtmlUtil.getHtml(url, "gb2312");
        Pattern pattern = Pattern.compile("playurl_flash=\"(.+?)\"");
        Matcher m = pattern.matcher(chtml);
        if (m.find()) {
            String link = null;
            try {
                link = com.unclezs.Utils.URLEncoder.encode("http://www.tingchina.com" + m.group(1), "gb2312");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            String html = HtmlUtil.getSource(link, "gb2312", url, null);
            Pattern p = Pattern.compile("url.3.= \"(.+?)\"");
            Matcher mm = p.matcher(html);
            if (mm.find()) {
                String res = "http://t44.tingchina.com" + mm.group(1);
                return res;
            }
        }
        return "";
    }

    //懒人听书
    public String getLRTS(String url) {
        String html = HtmlUtil.getHtml(url, "utf-8");
        Document document = Jsoup.parse(html);
        String realUrl = document.select(".section").first().select("input").first().attr("value");
        if (realUrl.length() < 4)
            realUrl = "http://180d.ysts8.com:8000/";
        return realUrl;
    }

    //22听书网
    public String getTING22(String url) {
        String bookId = url.substring(url.lastIndexOf("/") + 1, url.indexOf("-"));
        int chapter_number = Integer.parseInt(url.substring(url.lastIndexOf("-") + 1, url.indexOf(".ht")));
        int page=chapter_number/10;
        if(chapter_number/10==0){
            page+=1;
        }
        if(chapter_number%10==0){
            page-=1;
        }

        int pageIndex = (chapter_number-1) % 10;
        //音频地址API
        String api = "https://www.ting22.com/api.php?c=Json&page="+page+"&pagesize=10&callback=unclezs&id=" + bookId;
        Map<String, String> headers = new HashMap<>(2);
        headers.put(Config.Referer, url);
        headers.put("sign", new Date().getTime() + "");
        //获取URL加密Code
        String code = JSON.parseObject(HtmlUtil.getHtmlSource(api, "utf-8", headers).substring(8).replace(");", "")).getJSONArray("playlist").getJSONObject(pageIndex).getString("file");
        //解密
        code=getCodeString(code);
        //根据类型获取真实音频地址
        String realUrl="";
        String[] tempUrl = code.split("[$]");
        if(tempUrl.length==2){//不是默认真实音频地址的，需要另外解析
            switch (tempUrl[1]){
                case "tc"://yousheng/29545/0$tc
                    String[] data = tempUrl[0].split("/");
                    String tc_api=data[0] + "/" + data[1] + "/play_" + data[1] + "_" + data[2] + ".htm";
                    realUrl = JSON.parseObject(HtmlUtil.getHtml("https://c.ting22.com/tingchina.php?file="+tc_api, "utf-8")).getString("url");
                    break;
                case "xm":// 19576598/139592587$xm
                    realUrl="http://mobile.ximalaya.com/mobile/redirect/free/play/"+tempUrl[0].split("/")[1]+"/0";
                    break;
            }
        }else {
            realUrl=tempUrl[0];
        }
        return realUrl;
    }

    public List<AudioChapter> getLRTSChapters(String url) {
        List<AudioChapter> chapters = new ArrayList<>(100);
        String bookId = url.substring(url.lastIndexOf("/") + 1);
        String html = HtmlUtil.getHtml(url, "utf-8");
        Pattern pattern = Pattern.compile("章节：</span>(.+?)</li>");
        Matcher m = pattern.matcher(html);
        m.find();
        int pageNum = Integer.parseInt(m.group(1));//章节总数
        for (int i = 1; i <= pageNum; i++) {
            chapters.add(new AudioChapter("http://www.lrts.me/ajax/playlist/2/" + bookId + "/" + i, "" + i));
        }
        return chapters;
    }

    //将45*48*78类的转字符
    private String getCodeString(String src){
        String[] u = src.split("[*]");
        String res = "";
        for (String s : u) {
            if (!"".equals(s))
                res += (char) Integer.parseInt(s);
        }
        return res;
    }
}
