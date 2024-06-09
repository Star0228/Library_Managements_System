<template>
  <el-scrollbar height="100%" style="width: 100%;">
    <!-- 标题和搜索框 -->
    <div style="margin-top: 20px; margin-left: 40px; font-size: 2em; font-weight: bold; ">图书管理
      <el-input v-model="toSearch" :prefix-icon="Search"
                style=" width: 15vw;min-width: 150px; margin-left: 30px; margin-right: 30px; float: right;" clearable />
    </div>
    <div style="margin-top: 20px; margin-left: 40px; font-size: 2em; font-weight: bold; ">
      <!-- 隐藏的file input，用于实际的文件选择 -->
      <input
          type="file"
          ref="fileInput"
          accept=".xlsx"
          style="display: none;"
          @change="Uploadbooks"
      />
      <!-- Element UI 的按钮，点击时触发文件选择 -->
      <el-button type="primary" @click="triggerFileInput">选择文件批量上传</el-button>
    </div>
    <div style="margin-top: 20px; margin-left: 40px; font-size: 2em; font-weight: bold; ">
      <!-- Element UI 的按钮，点击时触发 -->
      <el-button type="primary" @click="SiftVisible = true">图书筛选查看</el-button>
    </div>

    <!-- 图书卡片显示区 -->
    <div style="display: flex;flex-wrap: wrap; justify-content: start;">

      <!-- 图书卡片 -->
      <div class="bookBox" v-for="book in books" v-show="book.title.includes(toSearch)" :key="book.bookId">
        <div>
          <!-- 卡片标题 -->
          <div style="font-size: 25px; font-weight: bold;">No. {{ book.bookId }}</div>

          <el-divider />

          <!-- 卡片内容 -->
          <div style="margin-left: 10px; text-align: start; font-size: 16px;">
<!--            <p style="padding: 2.5px;"><span style="font-weight: bold;">序号：</span>{{ book.bookId }}</p>-->
            <p style="padding: 2.5px;"><span style="font-weight: bold;">书名：</span>{{ book.title }}</p>
            <p style="padding: 2.5px;"><span style="font-weight: bold;">风格：</span>{{ book.category }}</p>
            <p style="padding: 2.5px;"><span style="font-weight: bold;">作者：</span>{{ book.author }}</p>
            <p style="padding: 2.5px;"><span style="font-weight: bold;">出版年份：</span>{{ book.publishYear }}</p>
            <p style="padding: 2.5px;"><span style="font-weight: bold;">出版社：</span>{{ book.press }}</p>
            <p style="padding: 2.5px;"><span style="font-weight: bold;">价格：</span>{{ book.price }}</p>
            <p style="padding: 2.5px;"><span style="font-weight: bold;">库存：</span>{{ book.stock }}</p>
          </div>

          <el-divider />

          <!-- 卡片操作 -->
          <div style="margin-top: 1px;">
            <el-button type="primary" :icon="Edit" @click="this.toModifyInfo.bookId = book.bookId, this.toModifyInfo.title = book.title,
                this.toModifyInfo.category = book.category, this.toModifyInfo.author = book.author,
                this.toModifyInfo.press = book.press,this.toModifyInfo.publishYear = book.publishYear,
                this.toModifyInfo.price = book.price ,  modifyBookVisible = true" circle />

            <el-button type="success" :icon="Promotion" circle
                       @click="this.Toborrow.bookId = book.bookId ,this.newBorrowVisible = true"
                       style="margin-left: 30px;" />
            <el-button type="danger" :icon="Delete" circle
                       @click="this.toRemove = book.bookId, this.removeBookVisible = true"
                       style="margin-left: 30px;" />

          </div>

        </div>
      </div>


      <el-button class="newBookBox"
                 @click="newBookInfo.title = '', newBookInfo.category = '', newBookInfo.press = '',
                  newBookInfo.publishYear = '', newBookInfo.author = '', newBookInfo.price = '', newBookInfo.stock = '',
                  newBookVisible = true">
        <el-icon style="height: 50px; width: 50px;">
          <Plus style="height: 100%; width: 100%;" />
        </el-icon>
      </el-button>
    </div>


    <!-- 新建借书对话框 -->
    <el-dialog v-model="newBorrowVisible" title="借/还书申请" width="30%" align-center>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        借书证号：
        <el-input v-model="Toborrow.cardId" style="width: 12.5vw;" clearable />
      </div>

      <template #footer>
                <span>
                    <el-button @click="newBorrowVisible = false">取消</el-button>
                  <el-button type="primary" @click="ConfirmReturn"
                             :disabled="Toborrow.cardId.length === 0">确定还书</el-button>
                    <el-button type="primary" @click="ConfirmBorrow"
                               :disabled="Toborrow.cardId.length === 0">确定借书</el-button>
                </span>
      </template>
    </el-dialog>
    <!-- 新建筛选卡片 -->
    <el-dialog v-model="SiftVisible" title="筛选图书面板" width="30%" align-center>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        书名：
        <el-input v-model="condition.title" style="width: 12.5vw;" clearable />
      </div>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        风格：
        <el-input v-model="condition.category" style="width: 12.5vw;" clearable />
      </div>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        作者：
        <el-input v-model="condition.author" style="width: 12.5vw;" clearable />
      </div>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        出版社：
        <el-input v-model="condition.press" style="width: 12.5vw;" clearable />
      </div>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        最低价格：
        <el-input v-model="condition.minPrice" style="width: 12.5vw;" clearable />
      </div>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        最高价格：
        <el-input v-model="condition.maxPrice" style="width: 12.5vw;" clearable />
      </div>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        查询年份起始：
        <el-input v-model="condition.minPublishYear" style="width: 12.5vw;" clearable />
      </div>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        查询年份终止：
        <el-input v-model="condition.maxPublishYear" style="width: 12.5vw;" clearable />
      </div>
      <div style="margin-left: 2vw;   font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        排序关键词：
        <el-select v-model="condition.sortBy" size="default" style="width: 12.5vw;">
          <el-option v-for="type in stbytypes" :key="type.value" :label="type.label" :value="type.value" />
        </el-select>
      </div>
      <div style="margin-left: 2vw;   font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        排序方式：
        <el-select v-model="condition.sortOrder" size="default" style="width: 12.5vw;">
          <el-option v-for="type in stodtypes" :key="type.value" :label="type.label" :value="type.value" />
        </el-select>
      </div>

      <template #footer>
                <span>
                    <el-button @click="SiftVisible = false">取消</el-button>
                    <el-button type="primary" @click="QueryBooks">确定</el-button>
                </span>
      </template>
    </el-dialog>
    <!-- 新建图书对话框 -->
    <el-dialog v-model="newBookVisible" title="新建图书" width="30%" align-center>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        书名：
        <el-input v-model="newBookInfo.title" style="width: 12.5vw;" clearable />
      </div>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        风格：
        <el-input v-model="newBookInfo.category" style="width: 12.5vw;" clearable />
      </div>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        作者：
        <el-input v-model="newBookInfo.author" style="width: 12.5vw;" clearable />
      </div>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        出版年份：
        <el-input v-model="newBookInfo.publishYear" style="width: 12.5vw;" clearable />
      </div>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        出版社：
        <el-input v-model="newBookInfo.press" style="width: 12.5vw;" clearable />
      </div>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        价格：
        <el-input v-model="newBookInfo.price" style="width: 12.5vw;" clearable />
      </div>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        库存：
        <el-input v-model="newBookInfo.stock" style="width: 12.5vw;" clearable />
      </div>

      <template #footer>
                <span>
                    <el-button @click="newBookVisible = false">取消</el-button>
                    <el-button type="primary" @click="ConfirmNewBook"
                               :disabled="newBookInfo.title.length === 0 || newBookInfo.price.length === 0">确定</el-button>
                </span>
      </template>
    </el-dialog>

    <!-- 修改信息对话框 -->
    <el-dialog v-model="modifyBookVisible" :title="'修改信息(图书ID: ' + this.toModifyInfo.bookId + ')'" width="30%"
               align-center>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        库存变化(可正可负)：
        <el-input v-model="toModifyInfo.deltaStock" style="width: 12.5vw;" clearable />
      </div>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        书名：
        <el-input v-model="toModifyInfo.title" style="width: 12.5vw;" clearable />
      </div>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        风格：
        <el-input v-model="toModifyInfo.category" style="width: 12.5vw;" clearable />
      </div>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        作者：
        <el-input v-model="toModifyInfo.author" style="width: 12.5vw;" clearable />
      </div>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        出版年份：
        <el-input v-model="toModifyInfo.publishYear" style="width: 12.5vw;" clearable />
      </div>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        出版社：
        <el-input v-model="toModifyInfo.press" style="width: 12.5vw;" clearable />
      </div>
      <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
        价格：
        <el-input v-model="toModifyInfo.price" style="width: 12.5vw;" clearable />
      </div>
      <template #footer>
                <span class="dialog-footer">
                    <el-button @click="modifyBookVisible = false">取消</el-button>
                    <el-button type="primary" @click="ConfirmModifyBook"
                               :disabled="toModifyInfo.title.length === 0 || toModifyInfo.price.length === 0 ">确定(非库存部分)</el-button>
                  <el-button type="primary" @click="ConfirmBookStock"
                             :disabled="toModifyInfo.title.length === 0 || toModifyInfo.price.length === 0 ">确定(库存部分)</el-button>
                </span>
      </template>
    </el-dialog>

    <!-- 删除图书对话框 -->
    <el-dialog v-model="removeBookVisible" title="删除图书" width="30%">
      <span>确定删除<span style="font-weight: bold;">{{ toRemove }}号图书</span>吗？</span>

      <template #footer>
                <span class="dialog-footer">
                    <el-button @click="removeBookVisible = false">取消</el-button>
                    <el-button type="danger" @click="ConfirmRemoveBook">
                        删除
                    </el-button>
                </span>
      </template>
    </el-dialog>
  </el-scrollbar>
</template>

<script>
import {Delete, Edit, Search ,Promotion} from '@element-plus/icons-vue'

import {ElMessage} from 'element-plus'
import axios from 'axios'
import * as XLSX from 'xlsx';

export default {
  data() {
    return {
      books: [{ // 图书列表
        bookId: '',
        title: '平凡的世界',
        category: '',
        press: '',
        publishYear: '',
        author: '',
        price: '',
        stock: ''
      }
      ],
      condition:{
        category: null,
        title: null,
        press:null,
        minPublishYear: null,
        maxPublishYear: null,
        author:null,
        minPrice: null,
        maxPrice: null,
        sortBy: 'book_id',
        sortOrder:'asc'
      },
      stbytypes: [ // 借书证类型
        {
          value: 'book_id',
          label: '图书序号',
        },
        {
          value: 'category',
          label: '风格',
        },
        {
          value: 'title',
          label: '标题',
        },
        {
          value: 'press',
          label: '出版社',
        },
        {
          value: 'publish_year',
          label: '出版年份',
        },
        {
          value: 'price',
          label: '价格',
        },
        {
          value: 'stock',
          label: '库存',
        }
      ],
      stodtypes:[ // 借书证类型
        {
          value: 'asc',
          label: '升序',
        },
        {
          value: 'desc',
          label: '降序',
        }
      ],
      SiftVisible:false,
      Toborrow:{
        bookId:'',
        cardId:''
      },
      file: null,
      Delete,
      Edit,
      Search,
      Promotion,
      toSearch: '', // 搜索内容
      newBorrowVisible:false,
      newBookVisible: false, // 新建图书对话框可见性
      removeBookVisible: false, // 删除图书对话框可见性
      toRemove: 0, // 待删除图书号
      newBookInfo: { // 待新建图书信息
        title: '平凡的世界',
        category: '',
        press: '',
        publishYear: '',
        author: '',
        price: '',
        stock: ''
      },
      modifyBookVisible: false, // 修改信息对话框可见性
      toModifyInfo: { // 待修改图书信息
        bookId: '',
        title: '平凡的世界',
        category: '',
        press: '',
        publishYear: '',
        author: '',
        deltaStock: '',
        price: ''
      },
    }
  },
  methods: {
    ConfirmBorrow(){
      axios.post("/book/borrow",
          { // 请求体
            cardId:this.Toborrow.cardId,
            bookId:this.Toborrow.bookId
          })
          .then(response => {
            ElMessage.success("借书成功") // 显示消息提醒
            this.newBorrowVisible = false // 将对话框设置为不可见
            this.QueryBooks() // 重新查询图书以刷新页面
          }).catch(error => {
        ElMessage.error("借书失败")
        this.newBorrowVisible = false
        this.QueryBooks()
      })
    },
    ConfirmReturn(){
      axios.post("/book/return",
          { // 请求体
            cardId:this.Toborrow.cardId,
            bookId:this.Toborrow.bookId
          })
          .then(response => {
            ElMessage.success("还书成功") // 显示消息提醒
            this.newBorrowVisible = false // 将对话框设置为不可见
            this.QueryBooks() // 重新查询图书以刷新页面
          }).catch(error => {
        ElMessage.error("还书失败")
        this.newBorrowVisible = false
        this.QueryBooks()
      })
    },
    Uploadbooks(event) {
      this.file = event.target.files[0];
      if (!this.file) {
        ElMessage.error('No file selected.');
        this.file = null
        return;
      }
      const reader = new FileReader();
      reader.onload = (event) => {
        const data = new Uint8Array(event.target.result);
        const workbook = XLSX.read(data, {type: 'array'});

        const firstSheetName = workbook.SheetNames[0];
        const worksheet = workbook.Sheets[firstSheetName];
        const json = XLSX.utils.sheet_to_json(worksheet);

        // 使用Axios发送JSON数据到后端
        axios.post('/book/upload',
            {json}
        ).then(response => {
          ElMessage.success('批量入库成功');
          this.file = null
          this.QueryBooks() // 重新查询图书以刷新页面
        }).catch(error => {
          ElMessage.error('批量入库失败');
          this.file = null
          this.QueryBooks() // 重新查询图书以刷新页面
        });
      }
      reader.readAsArrayBuffer(this.file);
      this.file = null;
    },
    triggerFileInput() {
      // 点击按钮时，触发隐藏的file input的点击事件
      this.$refs.fileInput.click();
    },
    ConfirmNewBook() {
      // 发出POST请求
      axios.post("/book",
          { // 请求体
            title: this.newBookInfo.title,
            category: this.newBookInfo.category,
            press: this.newBookInfo.press,
            publishYear: this.newBookInfo.publishYear,
            author: this.newBookInfo.author,
            price: this.newBookInfo.price,
            stock: this.newBookInfo.stock
          })
          .then(response => {
            ElMessage.success("图书新建成功") // 显示消息提醒
            this.newBookVisible = false // 将对话框设置为不可见
            this.QueryBooks() // 重新查询图书以刷新页面
          }).catch(error => {
        ElMessage.error("图书新建失败")
        this.newBookVisible = false
        this.QueryBooks()
      })
    },
    ConfirmModifyBook() {
      // TODO: YOUR CODE HERE
      axios.post("/book/modify",
          {
            bookId: this.toModifyInfo.bookId,
            title: this.toModifyInfo.title,
            category: this.toModifyInfo.category,
            press: this.toModifyInfo.press,
            publishYear: this.toModifyInfo.publishYear,
            author: this.toModifyInfo.author,
            price: this.toModifyInfo.price,
          }).then(response => {
        ElMessage.success("图书修改成功")
        this.modifyBookVisible = false
        this.toModifyInfo.deltaStock = ''
        this.QueryBooks()
      }).catch(error => {
        ElMessage.error("图书修改失败")
        this.modifyBookVisible = false
        this.toModifyInfo.deltaStock = ''
        this.QueryBooks()
      })
    },
    ConfirmBookStock() {
      // TODO: YOUR CODE HERE
      axios.post("/book/modifystock",
          {
            deltaStock: this.toModifyInfo.deltaStock,
            bookId: this.toModifyInfo.bookId
          }).then(response => {
        ElMessage.success("库存修改成功")
        this.modifyBookVisible = false
        this.toModifyInfo.deltaStock = ''
        this.QueryBooks()
      }).catch(error => {
        ElMessage.error("库存修改失败")
        this.modifyBookVisible = false
        this.toModifyInfo.deltaStock = ''
        this.QueryBooks()
      })
    },
    ConfirmRemoveBook() {
      // TODO: YOUR CODE HERE
      axios.post("book/remove",
          {
            bookId: this.toRemove
          }).then(response => {
        ElMessage.success("图书删除完成")
        this.removeBookVisible = false
        this.QueryBooks()
      }).catch(error => {
        ElMessage.error("图书删除失败")
        this.removeBookVisible = false
        this.QueryBooks()
      })
    },
    clearpart(){
      this.condition.category=null
      this.condition.title= null
      this.condition.press=null
      this.condition.minPublishYear=null
      this.condition.maxPublishYear= null
      this.condition.author=null
      this.condition.minPrice= null
      this.condition.maxPrice= null
      this.condition.sortBy= 'book_id'
      this.condition.sortOrder='asc'
    },
    QueryBooks() {
      this.books = [] // 清空列表
      let response = axios.get('/book',{
        params: {
          category: this.condition.category,
          title: this.condition.title,
          press:this.condition.press,
          minPublishYear: this.condition.minPublishYear,
          maxPublishYear: this.condition.maxPublishYear,
          author:this.condition.author,
          minPrice: this.condition.minPrice,
          maxPrice: this.condition.maxPrice,
          sortBy: this.condition.sortBy,
          sortOrder: this.condition.sortOrder
        } }
      ) // 向/card发出GET请求
          .then(response => {
            let books = response.data // 接收响应负载
            books.forEach(book => { // 对于每个图书
              this.books.push(book) // 将其加入到列表中
            })
            this.SiftVisible = false
            this.clearpart()
          })
    }
  },

  mounted() { // 当页面被渲染时
    this.QueryBooks() // 查询图书
  }
}

</script>


<style scoped>
.bookBox {
  height: 420px;
  width: 275px;
  box-shadow: 0 4px 8px 0 rgba(0, 0, 0, 0.2), 0 6px 20px 0 rgba(0, 0, 0, 0.19);
  text-align: center;
  margin-top: 40px;
  margin-left: 27.5px;
  margin-right: 10px;
  padding: 7.5px;
  padding-right: 10px;
  padding-top: 15px;
}

.newBookBox {
  height: 420px;
  width: 275px;
  margin-top: 40px;
  margin-left: 27.5px;
  margin-right: 10px;
  padding: 7.5px;
  padding-right: 10px;
  padding-top: 15px;
  box-shadow: 0 4px 8px 0 rgba(0, 0, 0, 0.2), 0 6px 20px 0 rgba(0, 0, 0, 0.19);
  text-align: center;
}
</style>