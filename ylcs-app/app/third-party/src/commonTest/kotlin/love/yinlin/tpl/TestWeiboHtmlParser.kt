package love.yinlin.tpl

import kotlin.test.Test

class TestWeiboHtmlParser {
    companion object {
        const val TEXT1 = """
            <a  href="https://m.weibo.cn/p/index?extparam=%E9%93%B6%E4%B8%B4&containerid=10080848e33cc4065cd57c5503c2419cdea983&launchid=10000360-page_H5" data-hide=""><span class='url-icon'>
            <img style='width: 1rem;height: 1rem' src='https://n.sinaimg.cn/photo/5213b46e/20180926/timeline_card_small_super_default.png'></span><span class="surl-text">银临</span></a>
            <a  href="https://m.weibo.cn/search?containerid=231522type%3D1%26t%3D10%26q%3D%23%E5%A6%87%E5%A5%B3%E8%8A%82%23&isnewpage=1&launchid=10000360-page_H5" data-hide="">
            <span class="surl-text">#妇女节#</span></a>
            <a  href="https://m.weibo.cn/search?containerid=231522type%3D1%26t%3D10%26q%3D%23%E5%A6%87%E5%A5%B3%E8%8A%82%E5%BF%AB%E4%B9%90%23&isnewpage=1&launchid=10000360-page_H5" data-hide=""><span class="surl-text">#妇女节快乐#</span></a> <br />蹈涉深泉林海，向旷野奔逃；<br />飞越荒原冻雨，为梦境奔赴；<br />
            远征日月星空，与天地相逢；<br />“她”是我，是你，是我们，<br />可以翻腾出向上的波浪，<br />可以折射出万千的色彩，<br />祝“她”粼粼闪耀，万念俱辉，永远是自己。 
        """
        const val TEXT2 = """
            <a  href="https://m.weibo.cn/p/index?extparam=%E9%93%B6%E4%B8%B4&containerid=10080848e33cc4065cd57c5503c2419cdea983&launchid=10000360-page_H5" data-hide=""><span class='url-icon'>
            <img style='width: 1rem;height: 1rem' src='https://n.sinaimg.cn/photo/5213b46e/20180926/timeline_card_small_super_default.png'></span><span class="surl-text">银临</span></a>
            <a  href="https://m.weibo.cn/search?containerid=231522type%3D1%26t%3D10%26q%3D%23%E5%BC%82%E6%AC%A1%E4%B8%B4%E5%80%BE%2C%E6%AD%A5%E6%AD%A5%E5%94%AF%E9%93%B6%23&isnewpage=1&launchid=10000360-page_H5" data-hide=""><span class="surl-text">#异次临倾,步步唯银#</span></a>
            <a  href="https://m.weibo.cn/search?containerid=231522type%3D1%26t%3D10%26q%3D%23%E9%93%B6%E4%B8%B4%E7%B2%BC%E7%B2%BC%E5%B7%A1%E5%9B%9E%E6%BC%94%E5%94%B1%E4%BC%9A%23&extparam=%23%E9%93%B6%E4%B8%B4%E7%B2%BC%E7%B2%BC%E5%B7%A1%E5%9B%9E%E6%BC%94%E5%94%B1%E4%BC%9A%23&launchid=10000360-page_H5" data-hide="">
            <span class="surl-text">#银临粼粼巡回演唱会#</span></a> <br />粼！粼！粼！粼！<br />
            dy更新<span class="url-icon"><img alt="[yeah]" src="https://face.t.sinajs.cn/t4/appstyle/expression/ext/normal/56/2025_yeah_mobile.png" style="width:1em; height:1em;" /></span> 
        """

        const val TEXT3 = """
            <a  href="https://m.weibo.cn/p/index?extparam=%E9%93%B6%E4%B8%B4&containerid=10080848e33cc4065cd57c5503c2419cdea983&launchid=10000360-page_H5" data-hide="">
            <span class='url-icon'><img style='width: 1rem;height: 1rem' src='https://n.sinaimg.cn/photo/5213b46e/20180926/timeline_card_small_super_default.png'></span>
            <span class="surl-text">银临</span></a>
            <a  href="https://m.weibo.cn/search?containerid=231522type%3D1%26t%3D10%26q%3D%23%E5%BC%82%E6%AC%A1%E4%B8%B4%E5%80%BE%2C%E6%AD%A5%E6%AD%A5%E5%94%AF%E9%93%B6%23&isnewpage=1&launchid=10000360-page_H5" data-hide="">
            <span class="surl-text">#异次临倾,步步唯银#</span></a>
            <a  href="https://m.weibo.cn/search?containerid=231522type%3D1%26t%3D10%26q%3D%23%E9%93%B6%E4%B8%B4%E7%B2%BC%E7%B2%BC%E5%B7%A1%E5%9B%9E%E6%BC%94%E5%94%B1%E4%BC%9A%23&extparam=%23%E9%93%B6%E4%B8%B4%E7%B2%BC%E7%B2%BC%E5%B7%A1%E5%9B%9E%E6%BC%94%E5%94%B1%E4%BC%9A%23&launchid=10000360-page_H5" data-hide="">
            <span class="surl-text">#银临粼粼巡回演唱会#</span></a>
            广州站线下群来啦～<br /><a  href="https://m.weibo.cn/search?containerid=231522type%3D1%26t%3D10%26q%3D%23%E4%B8%80%E8%B5%B7%E6%9D%A5%E7%9C%8B%E6%BC%94%E5%94%B1%E4%BC%9A%23&launchid=10000360-page_H5" data-hide=""><span class="surl-text">#一起来看演唱会#</span></a> 
        """

        const val TEXT4 = """
            <a  href="https://m.weibo.cn/p/index?extparam=%E9%93%B6%E4%B8%B4&containerid=10080848e33cc4065cd57c5503c2419cdea983&launchid=10000360-page_H5" data-hide="">
            <span class='url-icon'><img style='width: 1rem;height: 1rem' src='https://n.sinaimg.cn/photo/5213b46e/20180926/timeline_card_small_super_default.png'></span>
            <span class="surl-text">银临</span></a> 拂落一身纷纭，重山风雪也轻盈。《粼粼》ep第四曲《天和山雨雪》正式上线，欢迎收听！
            <a  href="https://m.weibo.cn/search?containerid=231522type%3D1%26t%3D10%26q%3D%23%E9%93%B6%E4%B8%B4%23&isnewpage=1&launchid=10000360-page_H5" data-hide="">123</a>
            <a  href="https://m.weibo.cn/search?containerid=231522type%3D1%26t%3D10%26q%3D%23%E9%93%B6%E4%B8%B4ep%E7%B2%BC%E7%B2%BC%23&extparam=%23%E9%93%B6%E4%B8%B4ep%E7%B2%BC%E7%B2%BC%23&launchid=10000360-page_H5" data-hide="">
            <span class="surl-text">#银临ep粼粼#</span></a><a  href="https://m.weibo.cn/search?containerid=231522type%3D1%26t%3D10%26q%3D%23%E4%B8%83%E5%8F%B7%E6%89%93%E6%AD%8C%E4%B8%AD%E5%BF%83%23&extparam=%23%E4%B8%83%E5%8F%B7%E6%89%93%E6%AD%8C%E4%B8%AD%E5%BF%83%23&launchid=10000360-page_H5" data-hide="">
            <span class="surl-text">#七号打歌中心#</span></a>
        """

        const val TEXT5 = """
            欢迎<a href='/n/w司南喃喃喃'>@w司南喃喃喃</a> 共赴粼粼~期待现场新歌首唱🎤
            <a  href="https://m.weibo.cn/search?containerid=231522type%3D1%26t%3D10%26q%3D%23%E9%93%B6%E4%B8%B4%E7%B2%BC%E7%B2%BC%E5%B7%A1%E5%9B%9E%E6%BC%94%E5%94%B1%E4%BC%9A%23&extparam=%23%E9%93%B6%E4%B8%B4%E7%B2%BC%E7%B2%BC%E5%B7%A1%E5%9B%9E%E6%BC%94%E5%94%B1%E4%BC%9A%23&launchid=10000360-page_H5" data-hide="">
            <span class="surl-text">#银临粼粼巡回演唱会#</span></a><a  href="https://m.weibo.cn/search?containerid=231522type%3D1%26t%3D10%26q%3D%23%E4%B8%80%E8%B5%B7%E6%9D%A5%E7%9C%8B%E6%BC%94%E5%94%B1%E4%BC%9A%23&launchid=10000360-page_H5" data-hide=""><span class="surl-text">#一起来看演唱会#</span></a>
        """
    }

    @Test
    fun parseHtml() {
        println(weiboHtmlToRichString(TEXT1))
        println(weiboHtmlToRichString(TEXT2))
        println(weiboHtmlToRichString(TEXT3))
        println(weiboHtmlToRichString(TEXT4))
        println(weiboHtmlToRichString(TEXT5))
    }
}