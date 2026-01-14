#!/bin/bash

###############################################################################
# IM-Server 启动脚本
# 用途: 启动 IM 服务器的所有组件
# 使用方法: ./start.sh [service|tcp|all]
###############################################################################

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 项目根目录 (脚本在 scripts/ 子目录中)
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOG_DIR="$PROJECT_ROOT/logs"
PID_DIR="$PROJECT_ROOT/pids"

# 创建必要的目录
mkdir -p "$LOG_DIR"
mkdir -p "$PID_DIR"

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

# 检查中间件服务
check_middleware() {
    log_info "检查中间件服务状态..."
    
    local all_ok=true
    
    # 检查 MySQL
    if mysql -u root -p12345678 -e "SELECT 1" &>/dev/null; then
        log_success "MySQL 运行正常"
    else
        log_error "MySQL 未运行或连接失败"
        all_ok=false
    fi
    
    # 检查 Redis
    if redis-cli ping &>/dev/null; then
        log_success "Redis 运行正常"
    else
        log_error "Redis 未运行"
        all_ok=false
    fi
    
    # 检查 RabbitMQ
    if rabbitmqctl status &>/dev/null; then
        log_success "RabbitMQ 运行正常"
    else
        log_error "RabbitMQ 未运行"
        all_ok=false
    fi
    
    # 检查 Zookeeper
    if echo stat | nc localhost 2181 &>/dev/null; then
        log_success "Zookeeper 运行正常"
    else
        log_error "Zookeeper 未运行"
        all_ok=false
    fi
    
    if [ "$all_ok" = false ]; then
        log_error "部分中间件服务未运行，请先启动所有中间件服务"
        exit 1
    fi
    
    echo ""
}

# 编译项目
build_project() {
    log_info "检查项目编译状态..."
    
    # 检查是否需要编译
    if [ ! -d "$PROJECT_ROOT/common/target" ] || [ ! -d "$PROJECT_ROOT/codec/target" ]; then
        log_info "检测到项目未编译，正在编译所有模块..."
        cd "$PROJECT_ROOT"
        
        if mvn clean install -DskipTests > "$LOG_DIR/build.log" 2>&1; then
            log_success "项目编译成功"
        else
            log_error "项目编译失败，请检查日志: $LOG_DIR/build.log"
            exit 1
        fi
    else
        log_success "项目已编译"
    fi
    
    echo ""
}

# 启动 Service 模块
start_service() {
    log_info "正在启动 Service 模块..."
    
    # 检查是否已经运行
    if [ -f "$PID_DIR/service.pid" ]; then
        local pid=$(cat "$PID_DIR/service.pid")
        if ps -p $pid > /dev/null 2>&1; then
            log_warn "Service 模块已经在运行 (PID: $pid)"
            return 0
        fi
    fi
    
    cd "$PROJECT_ROOT/service"
    
    # 使用 Maven 启动
    nohup mvn spring-boot:run > "$LOG_DIR/service.log" 2>&1 &
    local pid=$!
    echo $pid > "$PID_DIR/service.pid"
    
    # 等待服务启动
    log_info "等待 Service 模块启动 (端口 8000)..."
    local count=0
    while [ $count -lt 30 ]; do
        if lsof -i:8000 &>/dev/null; then
            log_success "Service 模块启动成功 (PID: $pid)"
            log_info "日志文件: $LOG_DIR/service.log"
            return 0
        fi
        sleep 2
        count=$((count + 1))
        echo -n "."
    done
    
    echo ""
    log_error "Service 模块启动超时，请检查日志: $LOG_DIR/service.log"
    return 1
}

# 启动 Message-Store 模块
start_message_store() {
    log_info "正在启动 Message-Store 模块..."
    
    # 检查是否已经运行
    if [ -f "$PID_DIR/message-store.pid" ]; then
        local pid=$(cat "$PID_DIR/message-store.pid")
        if ps -p $pid > /dev/null 2>&1; then
            log_warn "Message-Store 模块已经在运行 (PID: $pid)"
            return 0
        fi
    fi
    
    cd "$PROJECT_ROOT/message-store"
    
    # 使用 Maven 启动
    nohup mvn spring-boot:run > "$LOG_DIR/message-store.log" 2>&1 &
    local pid=$!
    echo $pid > "$PID_DIR/message-store.pid"
    
    # Message-Store 没有固定端口，检查进程是否存在
    log_info "等待 Message-Store 模块启动..."
    sleep 5
    
    if ps -p $pid > /dev/null 2>&1; then
        log_success "Message-Store 模块启动成功 (PID: $pid)"
        log_info "日志文件: $LOG_DIR/message-store.log"
        return 0
    else
        log_error "Message-Store 模块启动失败，请检查日志: $LOG_DIR/message-store.log"
        return 1
    fi
}

# 启动 TCP 模块
start_tcp() {
    log_info "正在启动 TCP 模块..."
    
    # 检查是否已经运行
    if [ -f "$PID_DIR/tcp.pid" ]; then
        local pid=$(cat "$PID_DIR/tcp.pid")
        if ps -p $pid > /dev/null 2>&1; then
            log_warn "TCP 模块已经在运行 (PID: $pid)"
            return 0
        fi
    fi
    
    # 检查 Service 是否运行
    if ! lsof -i:8000 &>/dev/null; then
        log_error "Service 模块未运行，请先启动 Service 模块"
        return 1
    fi
    
    cd "$PROJECT_ROOT/tcp"
    
    # 使用 Maven 启动，传入配置文件路径
    nohup mvn spring-boot:run -Dspring-boot.run.arguments="src/main/resources/config.yml" > "$LOG_DIR/tcp.log" 2>&1 &
    local pid=$!
    echo $pid > "$PID_DIR/tcp.pid"
    
    # 等待服务启动
    log_info "等待 TCP 模块启动 (端口 19001, 19002)..."
    local count=0
    while [ $count -lt 30 ]; do
        if lsof -i:19001 &>/dev/null; then
            log_success "TCP 模块启动成功 (PID: $pid)"
            log_info "日志文件: $LOG_DIR/tcp.log"
            return 0
        fi
        sleep 2
        count=$((count + 1))
        echo -n "."
    done
    
    echo ""
    log_error "TCP 模块启动超时，请检查日志: $LOG_DIR/tcp.log"
    return 1
}

# 显示使用帮助
show_usage() {
    echo "用法: $0 [service|message-store|tcp|all]"
    echo ""
    echo "选项:"
    echo "  service        仅启动 Service 模块 (HTTP API 服务)"
    echo "  message-store  仅启动 Message-Store 模块 (消息存储服务)"
    echo "  tcp            仅启动 TCP 模块 (长连接网关)"
    echo "  all            启动所有模块 (默认)"
    echo ""
    echo "示例:"
    echo "  $0                # 启动所有模块"
    echo "  $0 service        # 仅启动 Service 模块"
    echo "  $0 message-store  # 仅启动 Message-Store 模块"
    echo "  $0 tcp            # 仅启动 TCP 模块"
}

# 主函数
main() {
    echo ""
    echo "=========================================="
    echo "  IM-Server 启动脚本"
    echo "=========================================="
    echo ""
    
    # 检查中间件
    check_middleware
    
    # 编译项目
    build_project
    
    case "${1:-all}" in
        service)
            start_service
            ;;
        message-store)
            start_message_store
            ;;
        tcp)
            start_tcp
            ;;
        all)
            start_service
            if [ $? -eq 0 ]; then
                echo ""
                sleep 3
                start_message_store
                echo ""
                sleep 3
                start_tcp
            fi
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
    log_success "启动完成！"
    echo "=========================================="
    echo ""
    echo "服务状态:"
    if lsof -i:8000 &>/dev/null; then
        echo -e "  ${GREEN}✓${NC} Service 模块: http://localhost:8000"
    else
        echo -e "  ${RED}✗${NC} Service 模块: 未运行"
    fi
    
    if [ -f "$PID_DIR/message-store.pid" ]; then
        local ms_pid=$(cat "$PID_DIR/message-store.pid")
        if ps -p $ms_pid > /dev/null 2>&1; then
            echo -e "  ${GREEN}✓${NC} Message-Store 模块: 运行中 (PID: $ms_pid)"
        else
            echo -e "  ${RED}✗${NC} Message-Store 模块: 未运行"
        fi
    else
        echo -e "  ${RED}✗${NC} Message-Store 模块: 未运行"
    fi
    
    if lsof -i:19001 &>/dev/null; then
        echo -e "  ${GREEN}✓${NC} TCP 模块: tcp://localhost:19001"
        echo -e "  ${GREEN}✓${NC} WebSocket 模块: ws://localhost:19002"
    else
        echo -e "  ${RED}✗${NC} TCP 模块: 未运行"
    fi
    echo ""
    echo "日志目录: $LOG_DIR"
    echo "PID 目录: $PID_DIR"
    echo ""
}

main "$@"
