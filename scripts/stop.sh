#!/bin/bash

###############################################################################
# IM-Server 停止脚本
# 用途: 停止 IM 服务器的所有组件
# 使用方法: ./stop.sh [service|tcp|all]
###############################################################################

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 项目根目录 (脚本在 scripts/ 子目录中)
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PID_DIR="$PROJECT_ROOT/pids"

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 停止进程
stop_process() {
    local name=$1
    local pid_file="$PID_DIR/${name}.pid"
    
    if [ ! -f "$pid_file" ]; then
        log_warn "${name} 模块未运行 (PID 文件不存在)"
        return 0
    fi
    
    local pid=$(cat "$pid_file")
    
    if ! ps -p $pid > /dev/null 2>&1; then
        log_warn "${name} 模块未运行 (进程不存在)"
        rm -f "$pid_file"
        return 0
    fi
    
    log_info "正在停止 ${name} 模块 (PID: $pid)..."
    
    # 发送 TERM 信号
    kill $pid 2>/dev/null
    
    # 等待进程结束
    local count=0
    while [ $count -lt 15 ]; do
        if ! ps -p $pid > /dev/null 2>&1; then
            log_success "${name} 模块已停止"
            rm -f "$pid_file"
            return 0
        fi
        sleep 1
        count=$((count + 1))
        echo -n "."
    done
    
    echo ""
    log_warn "${name} 模块未响应，尝试强制停止..."
    
    # 强制停止
    kill -9 $pid 2>/dev/null
    sleep 1
    
    if ! ps -p $pid > /dev/null 2>&1; then
        log_success "${name} 模块已强制停止"
        rm -f "$pid_file"
        return 0
    else
        log_error "${name} 模块停止失败"
        return 1
    fi
}

# 停止 TCP 模块
stop_tcp() {
    stop_process "tcp"
}

# 停止 Service 模块
stop_service() {
    stop_process "service"
}

# 停止所有模块
stop_all() {
    log_info "正在停止所有模块..."
    echo ""
    
    # 先停止 TCP，再停止 Message-Store，最后停止 Service
    stop_tcp
    echo ""
    stop_process "message-store"
    echo ""
    stop_service
}

# 清理所有相关进程
cleanup_all() {
    log_info "正在清理所有相关进程..."
    echo ""
    
    # 查找并停止所有 Maven 启动的 Spring Boot 进程
    local service_pids=$(lsof -ti:8000 2>/dev/null)
    if [ -n "$service_pids" ]; then
        log_info "发现 Service 模块进程 (端口 8000): $service_pids"
        kill $service_pids 2>/dev/null
        sleep 2
        kill -9 $service_pids 2>/dev/null
        log_success "Service 模块进程已清理"
    fi
    
    local tcp_pids=$(lsof -ti:19001 2>/dev/null)
    if [ -n "$tcp_pids" ]; then
        log_info "发现 TCP 模块进程 (端口 19001): $tcp_pids"
        kill $tcp_pids 2>/dev/null
        sleep 2
        kill -9 $tcp_pids 2>/dev/null
        log_success "TCP 模块进程已清理"
    fi
    
    local ws_pids=$(lsof -ti:19002 2>/dev/null)
    if [ -n "$ws_pids" ]; then
        log_info "发现 WebSocket 模块进程 (端口 19002): $ws_pids"
        kill $ws_pids 2>/dev/null
        sleep 2
        kill -9 $ws_pids 2>/dev/null
        log_success "WebSocket 模块进程已清理"
    fi
    
    # 清理 PID 文件
    rm -f "$PID_DIR"/*.pid
    
    echo ""
    log_success "所有进程已清理完成"
}

# 显示使用帮助
show_usage() {
    echo "用法: $0 [service|message-store|tcp|all|cleanup]"
    echo ""
    echo "选项:"
    echo "  service        仅停止 Service 模块"
    echo "  message-store  仅停止 Message-Store 模块"
    echo "  tcp            仅停止 TCP 模块"
    echo "  all            停止所有模块 (默认)"
    echo "  cleanup        强制清理所有相关进程 (包括端口占用)"
    echo ""
    echo "示例:"
    echo "  $0                # 停止所有模块"
    echo "  $0 service        # 仅停止 Service 模块"
    echo "  $0 message-store  # 仅停止 Message-Store 模块"
    echo "  $0 cleanup        # 强制清理所有进程"
}

# 主函数
main() {
    echo ""
    echo "=========================================="
    echo "  IM-Server 停止脚本"
    echo "=========================================="
    echo ""
    
    case "${1:-all}" in
        service)
            stop_service
            ;;
        message-store)
            stop_process "message-store"
            ;;
        tcp)
            stop_tcp
            ;;
        all)
            stop_all
            ;;
        cleanup)
            cleanup_all
            ;;
        -h|--help)
            show_usage
            exit 0
            ;;
        *)
            log_error "未知选项: $1"
            show_usage
            exit 1
            ;;
    esac
    
    echo ""
    echo "=========================================="
    log_success "停止完成！"
    echo "=========================================="
    echo ""
}

main "$@"
