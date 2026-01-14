#!/bin/bash

###############################################################################
# IM-Server 管理脚本
# 用途: 统一管理 IM 服务器的启动、停止、重启、状态查看
# 使用方法: ./im-server.sh {start|stop|restart|status|logs} [service|tcp|all]
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
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 启动服务
start_server() {
    local module=${1:-all}
    bash "$SCRIPT_DIR/start.sh" "$module"
}

# 停止服务
stop_server() {
    local module=${1:-all}
    bash "$SCRIPT_DIR/stop.sh" "$module"
}

# 重启服务
restart_server() {
    local module=${1:-all}
    log_info "正在重启 IM-Server..."
    echo ""
    stop_server "$module"
    echo ""
    sleep 2
    start_server "$module"
}

# 查看状态
show_status() {
    bash "$SCRIPT_DIR/status.sh"
}

# 查看日志
show_logs() {
    local module=${1:-all}
    local log_dir="$PROJECT_ROOT/logs"
    
    if [ ! -d "$log_dir" ]; then
        log_error "日志目录不存在: $log_dir"
        exit 1
    fi
    
    case "$module" in
        service)
            if [ -f "$log_dir/service.log" ]; then
                log_info "显示 Service 模块日志 (Ctrl+C 退出)..."
                tail -f "$log_dir/service.log"
            else
                log_error "Service 日志文件不存在"
            fi
            ;;
        tcp)
            if [ -f "$log_dir/tcp.log" ]; then
                log_info "显示 TCP 模块日志 (Ctrl+C 退出)..."
                tail -f "$log_dir/tcp.log"
            else
                log_error "TCP 日志文件不存在"
            fi
            ;;
        all)
            log_info "显示所有日志 (Ctrl+C 退出)..."
            if [ -f "$log_dir/service.log" ] && [ -f "$log_dir/tcp.log" ]; then
                tail -f "$log_dir/service.log" "$log_dir/tcp.log"
            elif [ -f "$log_dir/service.log" ]; then
                tail -f "$log_dir/service.log"
            elif [ -f "$log_dir/tcp.log" ]; then
                tail -f "$log_dir/tcp.log"
            else
                log_error "没有找到日志文件"
            fi
            ;;
        *)
            log_error "未知模块: $module"
            exit 1
            ;;
    esac
}

# 显示帮助信息
show_help() {
    cat << EOF

========================================
  IM-Server 管理脚本
========================================

用法: $0 {start|stop|restart|status|logs} [service|tcp|all]

命令:
  start      启动服务
  stop       停止服务
  restart    重启服务
  status     查看服务状态
  logs       查看服务日志

模块选项:
  service    Service 模块 (HTTP API 服务)
  tcp        TCP 模块 (长连接网关)
  all        所有模块 (默认)

示例:
  $0 start              # 启动所有模块
  $0 start service      # 仅启动 Service 模块
  $0 stop               # 停止所有模块
  $0 restart tcp        # 重启 TCP 模块
  $0 status             # 查看所有服务状态
  $0 logs service       # 查看 Service 模块日志

快捷命令:
  ./start.sh            # 等同于 $0 start
  ./stop.sh             # 等同于 $0 stop
  ./status.sh           # 等同于 $0 status

========================================

EOF
}

# 主函数
main() {
    local command=$1
    local module=${2:-all}
    
    case "$command" in
        start)
            start_server "$module"
            ;;
        stop)
            stop_server "$module"
            ;;
        restart)
            restart_server "$module"
            ;;
        status)
            show_status
            ;;
        logs)
            show_logs "$module"
            ;;
        -h|--help|help)
            show_help
            ;;
        *)
            if [ -z "$command" ]; then
                log_error "缺少命令参数"
            else
                log_error "未知命令: $command"
            fi
            show_help
            exit 1
            ;;
    esac
}

main "$@"
