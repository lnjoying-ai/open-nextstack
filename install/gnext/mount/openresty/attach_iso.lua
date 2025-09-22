
local ngx = ngx
local args = ngx.req.get_uri_args()
ngx.log(ngx.INFO,"uri_args: ",args["token"])
local headers = ngx.req.get_headers()
local vm_id = ngx.var.id
-- local backend_server = ngx.var.backend
-- local user_agent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36"
local user_agent = headers["User-Agent"]
ngx.log(ngx.INFO, "vm_id: ",vm_id)
local token = args["token"]
-- cookie = headers["Cookie"]
if (nil == token) then
    ak = args["X-Access-Key"]
    sec = args["X-Access-Secret"]
    if(nil == ak and nil == sec) then
            ngx.exit(444)
    end
end
local manger_url = ngx.var.manager_url
local url = manger_url..vm_id.."/iso"

local httpc = http.new()
local res,err = httpc:request_uri(url, {method = "GET", headers = {["x-access-token"]=token,
                                                                   ["User-Agent"] = user_agent,
                                                                   ["Accept"] = "*/*",
                                                                   }})
if not res then
    ngx.exit(444)
end

local json = cjson.decode(res.body)
ngx.log(ngx.INFO,"backend agent: ",json.agent)
if ( nil == json or nil ==json.token)then
        ngx.exit(444)
end
local uri = "/"..json.token
ngx.req.set_uri(uri, false)
ngx.req.set_uri_args("")
ngx.var.backend = "http://"..json.agent..":8000"
