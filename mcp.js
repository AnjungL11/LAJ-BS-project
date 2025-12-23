/**
 * Image Gallery MCP Server Bridge
 * 桥接脚本，用于连接Claude Desktop和本地Java后端
 */
import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from "@modelcontextprotocol/sdk/types.js";
import axios from "axios";

// 配置Java后端地址
const API_URL = "http://localhost:8080/api/mcp/search";

const server = new Server(
  {
    name: "image-gallery",
    version: "1.0.0",
  },
  {
    capabilities: {
      tools: {},
    },
  }
);

// 告诉Claude有哪些工具
server.setRequestHandler(ListToolsRequestSchema, async () => {
  return {
    tools: [
      {
        name: "search_images",
        description: "搜索本地图片库。支持关键词、标签、日期、相机型号。返回图片预览。",
        inputSchema: {
          type: "object",
          properties: {
            keyword: { type: "string", description: "关键词 (文件名或模糊匹配)" },
            tags: { type: "array", items: { type: "string" }, description: "标签列表" },
            cameraModel: { type: "string", description: "相机型号 (如 iPhone)" },
            startDate: { type: "string", description: "开始时间 (YYYY-MM-DD HH:mm:ss)" },
            endDate: { type: "string", description: "结束时间 (YYYY-MM-DD HH:mm:ss)" },
          },
        },
      },
    ],
  };
});

// 处理工具调用
server.setRequestHandler(CallToolRequestSchema, async (request) => {
  if (request.params.name === "search_images") {
    try {
      // 转发请求给Java后端
      const response = await axios.post(API_URL, request.params.arguments);
      
      // 返回结果给Claude
      return {
        content: [{ type: "text", text: response.data.data || "未找到结果" }],
      };
    } catch (error) {
      return {
        content: [{ type: "text", text: `后端连接失败: ${error.message}` }],
        isError: true,
      };
    }
  }
  throw new Error("工具不存在");
});

// 启动服务
const transport = new StdioServerTransport();
await server.connect(transport);
console.error("MCP Bridge running...");