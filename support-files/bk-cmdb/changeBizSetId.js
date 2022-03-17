
//请使用上一步骤中生成biz_set_list.json文件中的真实业务集数据替换此处的${biz_set_list}后再执行脚本
var jsonVal=${biz_set_list};

function changeBizSetID() {

    for (var p in jsonVal){

        let data = {
            $set: {bk_biz_set_id:jsonVal[p].biz_set_id}
        };

        db.cc_BizSetBase.updateOne({bk_biz_set_name:jsonVal[p].biz_set_name}, data);
    }
    
}
changeBizSetID()
