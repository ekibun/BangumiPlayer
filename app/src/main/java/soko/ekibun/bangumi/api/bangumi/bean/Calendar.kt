package soko.ekibun.bangumi.api.bangumi.bean

data class Calendar(var weekday: WeekdayBean? = null,
                    var items: List<Subject>? = null
) {

    /**
     * weekday : {"en":"Mon","cn":"星期一","ja":"月耀日","id":1}
     * items : [{"id":109956,"url":"http://bgm.tv/subject/109956","siteId":2,"name":"魔法少女 俺","name_cn":"魔法少女俺","summary":"","air_date":"2018-04-02","air_weekday":1,"rating":{"total":287,"count":{"10":5,"9":3,"8":38,"7":80,"6":100,"5":40,"4":9,"3":4,"2":2,"1":6},"score":6.3},"rank":3630,"images":{"large":"http://lain.bgm.tv/pic/cover/l/2d/0e/109956_b1Bnb.jpg","common":"http://lain.bgm.tv/pic/cover/c/2d/0e/109956_b1Bnb.jpg","medium":"http://lain.bgm.tv/pic/cover/m/2d/0e/109956_b1Bnb.jpg","small":"http://lain.bgm.tv/pic/cover/s/2d/0e/109956_b1Bnb.jpg","grid":"http://lain.bgm.tv/pic/cover/g/2d/0e/109956_b1Bnb.jpg"},"collection":{"doing":695}},{"id":206700,"url":"http://bgm.tv/subject/206700","siteId":2,"name":"実験品家族 -クリーチャーズ・ファミリー・デイズ-","name_cn":"实验品家庭","summary":"","air_date":"2018-04-09","air_weekday":1,"rating":{"total":61,"count":{"10":0,"9":0,"8":8,"7":15,"6":24,"5":6,"4":5,"3":1,"2":1,"1":1},"score":6},"rank":3138,"images":{"large":"http://lain.bgm.tv/pic/cover/l/3e/3b/206700_e1Izb.jpg","common":"http://lain.bgm.tv/pic/cover/c/3e/3b/206700_e1Izb.jpg","medium":"http://lain.bgm.tv/pic/cover/m/3e/3b/206700_e1Izb.jpg","small":"http://lain.bgm.tv/pic/cover/s/3e/3b/206700_e1Izb.jpg","grid":"http://lain.bgm.tv/pic/cover/g/3e/3b/206700_e1Izb.jpg"},"collection":{"doing":149}},{"id":221127,"url":"http://bgm.tv/subject/221127","siteId":2,"name":"ゴールデンカムイ","name_cn":"黄金神威","summary":"","air_date":"2018-04-09","air_weekday":1,"rating":{"total":162,"count":{"10":2,"9":5,"8":62,"7":65,"6":17,"5":7,"4":1,"3":1,"2":0,"1":2},"score":7.2},"rank":1507,"images":{"large":"http://lain.bgm.tv/pic/cover/l/0d/59/221127_v6nv6.jpg","common":"http://lain.bgm.tv/pic/cover/c/0d/59/221127_v6nv6.jpg","medium":"http://lain.bgm.tv/pic/cover/m/0d/59/221127_v6nv6.jpg","small":"http://lain.bgm.tv/pic/cover/s/0d/59/221127_v6nv6.jpg","grid":"http://lain.bgm.tv/pic/cover/g/0d/59/221127_v6nv6.jpg"},"collection":{"doing":620}},{"id":225022,"url":"http://bgm.tv/subject/225022","siteId":2,"name":"宇宙戦艦ティラミス","name_cn":"宇宙战舰提拉米斯","summary":"","air_date":"2018-04-02","air_weekday":1,"rating":{"total":166,"count":{"10":1,"9":1,"8":15,"7":56,"6":66,"5":20,"4":5,"3":0,"2":1,"1":1},"score":6.3},"rank":3332,"images":{"large":"http://lain.bgm.tv/pic/cover/l/94/21/225022_UWYZz.jpg","common":"http://lain.bgm.tv/pic/cover/c/94/21/225022_UWYZz.jpg","medium":"http://lain.bgm.tv/pic/cover/m/94/21/225022_UWYZz.jpg","small":"http://lain.bgm.tv/pic/cover/s/94/21/225022_UWYZz.jpg","grid":"http://lain.bgm.tv/pic/cover/g/94/21/225022_UWYZz.jpg"},"collection":{"doing":623}},{"id":228380,"url":"http://bgm.tv/subject/228380","siteId":2,"name":"ピアノの森","name_cn":"钢琴之森","summary":"","air_date":"2018-04-08","air_weekday":1,"rating":{"total":65,"count":{"10":0,"9":2,"8":15,"7":25,"6":15,"5":3,"4":1,"3":4,"2":0,"1":0},"score":6.7},"rank":2208,"images":{"large":"http://lain.bgm.tv/pic/cover/l/0b/7a/228380_XEVYv.jpg","common":"http://lain.bgm.tv/pic/cover/c/0b/7a/228380_XEVYv.jpg","medium":"http://lain.bgm.tv/pic/cover/m/0b/7a/228380_XEVYv.jpg","small":"http://lain.bgm.tv/pic/cover/s/0b/7a/228380_XEVYv.jpg","grid":"http://lain.bgm.tv/pic/cover/g/0b/7a/228380_XEVYv.jpg"},"collection":{"doing":338}},{"id":229393,"url":"http://bgm.tv/subject/229393","siteId":2,"name":"かくりよの宿飯","name_cn":"妖怪旅馆营业中","summary":"","air_date":"2018-04-02","air_weekday":1,"rating":{"total":42,"count":{"10":0,"9":0,"8":2,"7":22,"6":8,"5":7,"4":2,"3":1,"2":0,"1":0},"score":6.3},"images":{"large":"http://lain.bgm.tv/pic/cover/l/af/fa/229393_666ce.jpg","common":"http://lain.bgm.tv/pic/cover/c/af/fa/229393_666ce.jpg","medium":"http://lain.bgm.tv/pic/cover/m/af/fa/229393_666ce.jpg","small":"http://lain.bgm.tv/pic/cover/s/af/fa/229393_666ce.jpg","grid":"http://lain.bgm.tv/pic/cover/g/af/fa/229393_666ce.jpg"},"collection":{"doing":155}},{"id":231971,"url":"http://bgm.tv/subject/231971","siteId":2,"name":"ベイブレード バースト 超ゼツ","name_cn":"战斗陀螺 爆烈 超绝","summary":"","air_date":"2018-04-02","air_weekday":1,"images":{"large":"http://lain.bgm.tv/pic/cover/l/28/c2/231971_eAn8a.jpg","common":"http://lain.bgm.tv/pic/cover/c/28/c2/231971_eAn8a.jpg","medium":"http://lain.bgm.tv/pic/cover/m/28/c2/231971_eAn8a.jpg","small":"http://lain.bgm.tv/pic/cover/s/28/c2/231971_eAn8a.jpg","grid":"http://lain.bgm.tv/pic/cover/g/28/c2/231971_eAn8a.jpg"}},{"id":234378,"url":"http://bgm.tv/subject/234378","siteId":2,"name":"美男高校地球防衛部 HAPPY KISS!","name_cn":"美男高校地球防卫部 HAPPY KISS！","summary":"","air_date":"2018-04-08","air_weekday":1,"rating":{"total":7,"count":{"10":0,"9":1,"8":1,"7":2,"6":1,"5":1,"4":0,"3":1,"2":0,"1":0},"score":6.4},"images":{"large":"http://lain.bgm.tv/pic/cover/l/06/af/234378_lj53r.jpg","common":"http://lain.bgm.tv/pic/cover/c/06/af/234378_lj53r.jpg","medium":"http://lain.bgm.tv/pic/cover/m/06/af/234378_lj53r.jpg","small":"http://lain.bgm.tv/pic/cover/s/06/af/234378_lj53r.jpg","grid":"http://lain.bgm.tv/pic/cover/g/06/af/234378_lj53r.jpg"},"collection":{"doing":37}},{"id":235016,"url":"http://bgm.tv/subject/235016","siteId":2,"name":"パズドラ","name_cn":"智龙迷城","summary":"","air_date":"2018-04-02","air_weekday":1,"rating":{"total":2,"count":{"10":0,"9":0,"8":1,"7":0,"6":1,"5":0,"4":0,"3":0,"2":0,"1":0},"score":7},"images":{"large":"http://lain.bgm.tv/pic/cover/l/9c/e7/235016_S2b2O.jpg","common":"http://lain.bgm.tv/pic/cover/c/9c/e7/235016_S2b2O.jpg","medium":"http://lain.bgm.tv/pic/cover/m/9c/e7/235016_S2b2O.jpg","small":"http://lain.bgm.tv/pic/cover/s/9c/e7/235016_S2b2O.jpg","grid":"http://lain.bgm.tv/pic/cover/g/9c/e7/235016_S2b2O.jpg"},"collection":{"doing":9}},{"id":236103,"url":"http://bgm.tv/subject/236103","siteId":2,"name":"踏切時間","name_cn":"道口时间","summary":"","air_date":"2018-04-09","air_weekday":1,"rating":{"total":93,"count":{"10":1,"9":2,"8":14,"7":34,"6":33,"5":6,"4":1,"3":1,"2":0,"1":1},"score":6.6},"rank":2536,"images":{"large":"http://lain.bgm.tv/pic/cover/l/f4/c7/236103_kuvkS.jpg","common":"http://lain.bgm.tv/pic/cover/c/f4/c7/236103_kuvkS.jpg","medium":"http://lain.bgm.tv/pic/cover/m/f4/c7/236103_kuvkS.jpg","small":"http://lain.bgm.tv/pic/cover/s/f4/c7/236103_kuvkS.jpg","grid":"http://lain.bgm.tv/pic/cover/g/f4/c7/236103_kuvkS.jpg"},"collection":{"doing":355}},{"id":237046,"url":"http://bgm.tv/subject/237046","siteId":2,"name":"お前はまだグンマを知らない","name_cn":"你还是不懂群马","summary":"","air_date":"2018-04-02","air_weekday":1,"rating":{"total":58,"count":{"10":0,"9":0,"8":0,"7":6,"6":27,"5":18,"4":5,"3":0,"2":2,"1":0},"score":5.5},"rank":3852,"images":{"large":"http://lain.bgm.tv/pic/cover/l/6d/ab/237046_St35v.jpg","common":"http://lain.bgm.tv/pic/cover/c/6d/ab/237046_St35v.jpg","medium":"http://lain.bgm.tv/pic/cover/m/6d/ab/237046_St35v.jpg","small":"http://lain.bgm.tv/pic/cover/s/6d/ab/237046_St35v.jpg","grid":"http://lain.bgm.tv/pic/cover/g/6d/ab/237046_St35v.jpg"},"collection":{"doing":221}},{"id":239747,"url":"http://bgm.tv/subject/239747","siteId":2,"name":"キャラとおたまじゃくし島","name_cn":"","summary":"","air_date":"2018-04-02","air_weekday":1,"images":{"large":"http://lain.bgm.tv/pic/cover/l/33/d5/239747_q05ea.jpg","common":"http://lain.bgm.tv/pic/cover/c/33/d5/239747_q05ea.jpg","medium":"http://lain.bgm.tv/pic/cover/m/33/d5/239747_q05ea.jpg","small":"http://lain.bgm.tv/pic/cover/s/33/d5/239747_q05ea.jpg","grid":"http://lain.bgm.tv/pic/cover/g/33/d5/239747_q05ea.jpg"}},{"id":239962,"url":"http://bgm.tv/subject/239962","siteId":2,"name":"レディスポ","name_cn":"Lady Sport","summary":"","air_date":"2018-04-09","air_weekday":1,"images":{"large":"http://lain.bgm.tv/pic/cover/l/70/58/239962_g98e8.jpg","common":"http://lain.bgm.tv/pic/cover/c/70/58/239962_g98e8.jpg","medium":"http://lain.bgm.tv/pic/cover/m/70/58/239962_g98e8.jpg","small":"http://lain.bgm.tv/pic/cover/s/70/58/239962_g98e8.jpg","grid":"http://lain.bgm.tv/pic/cover/g/70/58/239962_g98e8.jpg"},"collection":{"doing":13}},{"id":241879,"url":"http://bgm.tv/subject/241879","siteId":2,"name":"わしも-wasimo- 第6シリーズ","name_cn":"","summary":"","air_date":"2018-04-02","air_weekday":1,"images":{"large":"http://lain.bgm.tv/pic/cover/l/29/ad/241879_2uzku.jpg","common":"http://lain.bgm.tv/pic/cover/c/29/ad/241879_2uzku.jpg","medium":"http://lain.bgm.tv/pic/cover/m/29/ad/241879_2uzku.jpg","small":"http://lain.bgm.tv/pic/cover/s/29/ad/241879_2uzku.jpg","grid":"http://lain.bgm.tv/pic/cover/g/29/ad/241879_2uzku.jpg"}},{"id":241967,"url":"http://bgm.tv/subject/241967","siteId":2,"name":"Four of a kind","name_cn":"四牌士","summary":"","air_date":"2018-04-02","air_weekday":1,"rating":{"total":6,"count":{"10":0,"9":0,"8":2,"7":2,"6":0,"5":1,"4":0,"3":1,"2":0,"1":0},"score":6.3},"images":{"large":"http://lain.bgm.tv/pic/cover/l/11/22/241967_S2EB2.jpg","common":"http://lain.bgm.tv/pic/cover/c/11/22/241967_S2EB2.jpg","medium":"http://lain.bgm.tv/pic/cover/m/11/22/241967_S2EB2.jpg","small":"http://lain.bgm.tv/pic/cover/s/11/22/241967_S2EB2.jpg","grid":"http://lain.bgm.tv/pic/cover/g/11/22/241967_S2EB2.jpg"},"collection":{"doing":22}},{"id":242908,"url":"http://bgm.tv/subject/242908","siteId":2,"name":"帝王攻略","name_cn":"帝王攻略","summary":"","air_date":"2018-04-30","air_weekday":1,"images":{"large":"http://lain.bgm.tv/pic/cover/l/75/56/242908_Yh8hr.jpg","common":"http://lain.bgm.tv/pic/cover/c/75/56/242908_Yh8hr.jpg","medium":"http://lain.bgm.tv/pic/cover/m/75/56/242908_Yh8hr.jpg","small":"http://lain.bgm.tv/pic/cover/s/75/56/242908_Yh8hr.jpg","grid":"http://lain.bgm.tv/pic/cover/g/75/56/242908_Yh8hr.jpg"},"collection":{"doing":6}},{"id":242963,"url":"http://bgm.tv/subject/242963","siteId":2,"name":"小兵・杨来西 第二部","name_cn":"小兵杨来西 第二部","summary":"","air_date":"2018-04-16","air_weekday":1,"images":{"large":"http://lain.bgm.tv/pic/cover/l/6e/20/242963_mLpuw.jpg","common":"http://lain.bgm.tv/pic/cover/c/6e/20/242963_mLpuw.jpg","medium":"http://lain.bgm.tv/pic/cover/m/6e/20/242963_mLpuw.jpg","small":"http://lain.bgm.tv/pic/cover/s/6e/20/242963_mLpuw.jpg","grid":"http://lain.bgm.tv/pic/cover/g/6e/20/242963_mLpuw.jpg"}},{"id":242971,"url":"http://bgm.tv/subject/242971","siteId":2,"name":"飞天少年 第二季","name_cn":"飞天少年之启航篇","summary":"","air_date":"2018-04-09","air_weekday":1,"rating":{"total":1,"count":{"10":0,"9":0,"8":0,"7":0,"6":0,"5":0,"4":0,"3":0,"2":0,"1":1},"score":1},"images":{"large":"http://lain.bgm.tv/pic/cover/l/4c/8e/242971_qgUOQ.jpg","common":"http://lain.bgm.tv/pic/cover/c/4c/8e/242971_qgUOQ.jpg","medium":"http://lain.bgm.tv/pic/cover/m/4c/8e/242971_qgUOQ.jpg","small":"http://lain.bgm.tv/pic/cover/s/4c/8e/242971_qgUOQ.jpg","grid":"http://lain.bgm.tv/pic/cover/g/4c/8e/242971_qgUOQ.jpg"}},{"id":243034,"url":"http://bgm.tv/subject/243034","siteId":2,"name":"天眼归来","name_cn":"天眼归来","summary":"","air_date":"2018-04-16","air_weekday":1,"rating":{"total":1,"count":{"10":0,"9":0,"8":0,"7":0,"6":0,"5":0,"4":0,"3":0,"2":0,"1":1},"score":1},"images":{"large":"http://lain.bgm.tv/pic/cover/l/14/e2/243034_QXP3m.jpg","common":"http://lain.bgm.tv/pic/cover/c/14/e2/243034_QXP3m.jpg","medium":"http://lain.bgm.tv/pic/cover/m/14/e2/243034_QXP3m.jpg","small":"http://lain.bgm.tv/pic/cover/s/14/e2/243034_QXP3m.jpg","grid":"http://lain.bgm.tv/pic/cover/g/14/e2/243034_QXP3m.jpg"}},{"id":243097,"url":"http://bgm.tv/subject/243097","siteId":2,"name":"ゴールデン道画劇場","name_cn":"黄金神威 小剧场","summary":"","air_date":"2018-04-16","air_weekday":1,"rating":{"total":7,"count":{"10":0,"9":0,"8":0,"7":4,"6":2,"5":0,"4":0,"3":0,"2":1,"1":0},"score":6},"images":{"large":"http://lain.bgm.tv/pic/cover/l/e1/36/243097_X8qPp.jpg","common":"http://lain.bgm.tv/pic/cover/c/e1/36/243097_X8qPp.jpg","medium":"http://lain.bgm.tv/pic/cover/m/e1/36/243097_X8qPp.jpg","small":"http://lain.bgm.tv/pic/cover/s/e1/36/243097_X8qPp.jpg","grid":"http://lain.bgm.tv/pic/cover/g/e1/36/243097_X8qPp.jpg"},"collection":{"doing":22}},{"id":243166,"url":"http://bgm.tv/subject/243166","siteId":2,"name":"三只松鼠","name_cn":"三只松鼠","summary":"","air_date":"2018-04-09","air_weekday":1,"images":{"large":"http://lain.bgm.tv/pic/cover/l/34/00/243166_yWH3q.jpg","common":"http://lain.bgm.tv/pic/cover/c/34/00/243166_yWH3q.jpg","medium":"http://lain.bgm.tv/pic/cover/m/34/00/243166_yWH3q.jpg","small":"http://lain.bgm.tv/pic/cover/s/34/00/243166_yWH3q.jpg","grid":"http://lain.bgm.tv/pic/cover/g/34/00/243166_yWH3q.jpg"}},{"id":243887,"url":"http://bgm.tv/subject/243887","siteId":2,"name":"奇奇怪怪","name_cn":"奇奇怪怪","summary":"","air_date":"2018-04-23","air_weekday":1,"images":{"large":"http://lain.bgm.tv/pic/cover/l/f1/03/243887_O4OaC.jpg","common":"http://lain.bgm.tv/pic/cover/c/f1/03/243887_O4OaC.jpg","medium":"http://lain.bgm.tv/pic/cover/m/f1/03/243887_O4OaC.jpg","small":"http://lain.bgm.tv/pic/cover/s/f1/03/243887_O4OaC.jpg","grid":"http://lain.bgm.tv/pic/cover/g/f1/03/243887_O4OaC.jpg"}}]
     */

    class WeekdayBean {
        /**
         * en : Mon
         * cn : 星期一
         * ja : 月耀日
         * id : 1
         */

        //var en: String? = null
        //var cn: String? = null
        //var ja: String? = null
        var id: Int = 0
    }
}