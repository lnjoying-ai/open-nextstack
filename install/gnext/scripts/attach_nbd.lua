local ngx = ngx
local args = ngx.req.get_uri_args()
local file = args["file"]
local box_headers = ngx.req.get_headers()
local box_id = ngx.var.id
local cookie = box_headers["Cookie"]
if (nil==cookie) then
    local authorization = box_header["Authorization"]
end
-- ngx.log(ngx.INFO, "cookie: ",cookie)
local manger_url = ngx.var.manager_url
local url = manger_url..box_id.."/nbd"

local httpc = http.new()
-- local res,err = httpc:request_uri(url, {method = "GET", headers = box_headers
local res,err
if (nil==cookie) then

    res,err = httpc:request_uri(url,{method = "GET", headers = {["Authorization"]=authorization,
                                                             ["Accept"] = "*/*"}})
else 
    res,err = httpc:request_uri(url,{method = "GET", headers = {
                                                                 ["Cookie"]=cookie,
                                                             ["Accept"] = "*/*"}})
end
-- local res,err = httpc:request_uri(url, {method = "GET"})

if not res then
    ngx.exit(444)
end

ngx.log(ngx.INFO,"file: ",file)
local json = cjson.decode(res.body)
if ( nil == json or nil ==json.nbd_port)then
        ngx.exit(444)
end
-- local uri = "/"..json.token
-- ngx.req.set_uri(uri, false)
-- ngx.req.set_uri_args("file="..file)
ngx.var.backend = "http://127.0.0.1:"..json.nbd_port.."/"..box_id.."?file="..file
ngx.log(ngx.INFO, "backend: ", ngx.var.backend)
