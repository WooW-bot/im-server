#!/bin/bash

###############################################################################
# IM-Server 状态检查脚本
# 用途: 检查 IM 服务器所有组件的运行状态
# 使用方法: ./status.sh
###############################################################################

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 项目根目录 (脚本在 scripts/ 子目录中)
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PID_DIR="$PROJECT_ROOT/pids"

# 检查服务状态
check_service_status() {
    local name=$1
    local port=$2
    local pid_file="$PID_DIR/${name}.pid"
    
    echo -n "  ${name} 模块: "
    
    # 检查端口
    if lsof -i:$port &>/dev/null; then
        local pid=$(lsof -ti:$port)
        echo -e "${GREEN}运行中${NC} (PID: $pid, 端口: $port)"
        
        # 检查 PID 文件是否一致
        if [ -f "$pid_file" ]; then
            local saved_pid=$(cat "$pid_file")
            if [ "$pid" != "$saved_pid" ]; then
                echo -e "    ${YELLOW}警告: PID 文件不一致 (文件: $saved_pid, 实际: $pid)${NC}"
            fi
        else
            echo -e "    ${YELLOW}警告: PID 文件不存在${NC}"
        fi
        return 0
    else
        echo -e "${RED}未运行${NC}"
        if [ -f "$pid_file" ]; then
            echo -e "    ${YELLOW}警告: PID 文件存在但进程未运行${NC}"
        fi
        return 1
    fi
}

# 检查中间件状态
check_middleware_status() {
    local name=$1
    local check_cmd=$2
    
    echo -n "  ${name}: "
    
    if eval "$check_cmd" &>/dev/null; then
        echo -e "${GREEN}运行中${NC}"
        return 0
    else
        echo -e "${RED}未运行${NC}"
        return 1
    fi
}

# 主函数
main() {
    echo ""
    echo "=========================================="
    echo "  IM-Server 状态检查"
    echo "=========================================="
    echo ""
    
    # 检查中间件服务
    echo -e "${CYAN}中间件服务状态:${NC}"
    check_middleware_status "MySQL      " "mysql -u root -p12345678 -e 'SELECT 1'"
    check_middleware_status "Redis      " "redis-cli ping"
    check_middleware_status "RabbitMQ   " "rabbitmqctl status"
    check_middleware_status "Zookeeper  " "echo stat | nc localhost 2181"
    
    echo ""
    echo -e "${CYAN}应用服务状态:${NC}"
    
    # 检查应用服务
    local service_running=false
    local message_store_running=false
    local tcp_running=false
    
    if check_service_status "Service" "8000"; then
        service_running=true
    fi
    
    # 检查 Message-Store 模块 (没有固定端口，检查 PID)
    echo -n "  Message-Store 模块: "
    if [ -f "$PID_DIR/message-store.pid" ]; then
        local ms_pid=$(cat "$PID_DIR/message-store.pid")
        if ps -p $ms_pid > /dev/null 2>&1; then
            echo -e "${GREEN}运行中${NC} (PID: $ms_pid)"
            message_store_running=true
        else
            echo -e "${RED}未运行${NC}"
            echo -e "    ${YELLOW}警告: PID 文件存在但进程未运行${NC}"
        fi
    else
        echo -e "${RED}未运行${NC}"
    fi
    
    if check_service_status "TCP" "19001"; then
        tcp_running=true
    fi
    
    # 检查 WebSocket 端口
    echo -n "  WebSocket 模块: "
    if lsof -i:19002 &>/dev/null; then
        echo -e "${GREEN}运行中${NC} (端口: 19002)"
    else
        echo -e "${RED}未运行${NC}"
    fi
    
    echo ""
    echo "=========================================="
    
    # 显示访问地址
    if [ "$service_running" = true ]; then
        echo -e "${GREEN}Service API:${NC} http://localhost:8000"
        echo -e "  登录接口: POST http://localhost:8000/v1/user/login"
    fi
    
    if [ "$tcp_running" = true ]; then
        echo -e "${GREEN}TCP 网关:${NC} tcp://localhost:19001"
        echo -e "${GREEN}WebSocket:${NC} ws://localhost:19002"
    fi
    
    echo "=========================================="
    echo ""
    
    # 显示日志位置
    if [ -d "$PROJECT_ROOT/logs" ]; then
        echo "日志目录: $PROJECT_ROOT/logs"
        if [ -f "$PROJECT_ROOT/logs/service.log" ]; then
            echo "  Service 日志: tail -f $PROJECT_ROOT/logs/service.log"
        fi
        if [ -f "$PROJECT_ROOT/logs/tcp.log" ]; then
            echo "  TCP 日志: tail -f $PROJECT_ROOT/logs/tcp.log"
        fi
        echo ""
    fi
}

main "$@"
