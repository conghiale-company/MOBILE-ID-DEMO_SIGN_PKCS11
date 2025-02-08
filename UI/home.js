document.addEventListener('DOMContentLoaded', function() {
    loadKeyStores()
});

const loadKeyStores = () => {
    axios.get('http://localhost:8080/DemoSignWithUSBToken/api/signature')
        .then(function (response) {
            const data = response.data;
            updateTable(data);
        })
        .catch(function (error) {
            console.error('Error:', error);
        })
        .then(function () {
            // Luôn được thực thi, giống như finally
        })
}

function updateTable(data) {
    const tbody = document.querySelector('.table tbody');
    tbody.innerHTML = ''; // Xóa các hàng hiện có trong bảng

    data.forEach((item, index) => {
        const row = document.createElement('tr');
            row.innerHTML = `
                <th scope="row">
                    <text class="d-flex justify-content-center align-items-center">${index + 1}</text>
                </th>
                <td>${item.alias}</td>
                <td>${item.serialNumber}</td>
                <td>${item.information}</td>
                <td >
                    <button 
                        type="button" 
                        class="btn btn-danger btn-sm px-3 btnRemove"
                        data-bs-toggle="modal" 
                        data-bs-target="#removeModal"
                        data-bs-alias="${item.alias}"
                    >
                        Remove
                    </button>
                </td>
                <td >
                    <button 
                        type="button" 
                        class="btn btn-info btn-sm px-3 btnSign"
                        data-bs-toggle="modal" 
                        data-bs-target="#signModal"
                        data-bs-alias="${item.alias}"
                    >
                        Sign
                    </button>
                </td>
            `;
            tbody.appendChild(row);

            const btnRemove = row.querySelector('.btnRemove')
            btnRemove.addEventListener('click', function (event) {
                var button = event.target
                var recipient = button.getAttribute('data-bs-alias')

                const btnModalSendRemove = document.getElementById("btnModalSendRemove")
                btnModalSendRemove.setAttribute('data-bs-alias', recipient)

                const signModal = document.getElementById('signModal')
                signModal.setAttribute('data-bs-alias', recipient)
            })
    });
}

// handle when user click button send REMOVE key in modal
const btnModalSendRemove = document.getElementById('btnModalSendRemove')
btnModalSendRemove.addEventListener('click', function (event) {
    var button = event.target
    // Extract info from data-bs-* attributes
    // var alias = "Conghiale"
    var alias = button.getAttribute('data-bs-alias')

    axios.delete(`http://localhost:8080/DemoSignWithUSBToken/api/signature/${alias}`)
        .then(function (response) {
            const data = response.data;
            console.log(data);
            if (data === true) {
                let msg = `
                    <i class="fas fa-check-circle"></i> 
                    <text style="color: #0be881">KeyStore with alias <b style="color: #0be881"> ${alias} </b> deleted successfully </text>
                `
                loadKeyStores()
                showToast(msg)
            } else {
                let msg = `
                    <i class="fas fa-exclamation-circle"></i> 
                    <text style="color: #de123b">Cannot create keyStore with alias <b style="color: #de123b"> ${alias}</b> </text>
                `
                showToast(msg)
            }
        })
        .catch(function (error) {
            console.error('Error:', error);
        })
        .then(function () {
            // Luôn được thực thi, giống như finally
        })
})

// handle when user click button send CREATE key in modal
const btnModalSendCreate = document.getElementById('btnModalSendCreate')
btnModalSendCreate.addEventListener('click', function (event) {
    const alias = document.getElementById('alias').value
    const information = document.getElementById('information').value
    const extension = document.getElementById('extension').value

    if (alias !== "" && information !== "" && extension !== "") {
        axios.post(`http://localhost:8080/DemoSignWithUSBToken/api/signature`, {alias, information, extension})
            .then(function (response) {
                const data = response.data;
                if (data === true) {
                    let msg = `
                        <i class="fas fa-check-circle"></i> 
                        <text style="color: #0be881; margin-left: 10px;">KeyStore with alias <b style="color: #0be881"> ${alias} </b> created successfully </text>
                    `
                    loadKeyStores()
                    showToast(msg)
                } else {
                    let msg = `
                        <i class="fas fa-exclamation-circle"></i> 
                        <text style="color: #de123b; margin-left: 10px;">Cannot create keyStore with alias <b style="color: #de123b"> ${alias} </b></text>
                    `
                    showToast(msg)
                }
            })
            .catch(function (error) {
                console.error('Error:', error);
            })
            .then(function () {
                // Luôn được thực thi, giống như finally
            })
    } else {
        let msg = `
            <i class="fas fa-exclamation-triangle"></i> 
            <text style="color: #FFD43B; margin-left: 10px;">Data does not match. Please provide complete information.</text>
        `
        showToast(msg)
    }

})

// handle when user click button send SIGN key in modal
const btnModalSendSign = document.getElementById('btnModalSendSign')
btnModalSendSign.addEventListener('click', function (event) {
    event.preventDefault(); // Ngăn không cho form submit theo cách truyền thống

    var formData = new FormData();
    var alias = document.getElementById('_alias').value;
    var fileInput = document.getElementById('formFileMultiple');
    var file = fileInput.files[0];
    // var files = fileInput.files;

    formData.append('alias', alias);
    formData.append('fileName', file.name.split('.')[0]);
    formData.append('file', file);

    // console.log("LINE - 168: alias " + alias);
    // console.log("LINE - 169: file " + file.name + ", size: " + file.size); // Hiển thị tên và kích thước của file

    axios.post('http://localhost:8080/DemoSignWithUSBToken/api/sign', formData, {
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    })
    .then(function(response) {
        // Có thể đóng modal sau khi gửi thành công
        let msg = `
            <i class="fas fa-check-circle"></i> 
            <text style="color: #0be881">KeyStore with alias <b style="color: #0be881"> ${alias} </b> signed successfully </text>
        `
        showToast(msg)
        // Kiểm tra xem phản hồi có dữ liệu không
        // if (response && response.data) {
        //     // Tạo một Blob từ dữ liệu PDF
        //     var blob = new Blob([response.data], { type: 'application/pdf' });

        //     // Tạo link để tải file PDF
        //     var url = window.URL.createObjectURL(blob);
        //     var a = document.createElement('a');
        //     a.href = url;
        //     a.download = 'signed_document.pdf';
        //     document.body.appendChild(a);
        //     a.click();
        //     window.URL.revokeObjectURL(url);
        // } else {
        //     console.error('Invalid response:', response);
        //     showToast("Invalid response.");
        // }

        // $('#signModal').modal('hide');
        // Hiển thị nội dung của file PDF
        // displayPdfContent(blob);
    })
    .catch(function(error) {
        console.log('Error:', error);
        let msg = `
            <i class="fas fa-exclamation-circle"></i> 
            <text style="color: #de123b">${error}</text>
        `
        showToast(msg)
    });
})

// update content of CREATE modal
var createModal = document.getElementById('createModal')
createModal.addEventListener('show.bs.modal', function (event) {
// If necessary, you could initiate an AJAX request here
// and then do the updating in a callback.
    const alias = document.getElementById('alias').value = ""
    const information = document.getElementById('information').value = ""
    const extension = document.getElementById('extension').value = ""
    
})

// update content of SIGN modal
var signModal = document.getElementById('signModal')
signModal.addEventListener('show.bs.modal', function (event) {
    // Button that triggered the modal
    var button = event.relatedTarget
    // Extract info from data-bs-* attributes
    var recipient = button.getAttribute('data-bs-alias')

    document.getElementById("_alias").value = recipient
    document.getElementById("formFileMultiple").value = ''
})

// update content of REMOVE modal
var removeModal = document.getElementById('removeModal')
removeModal.addEventListener('show.bs.modal', function (event) {
    // Button that triggered the modal
    var button = event.relatedTarget
    // Extract info from data-bs-* attributes
    var recipient = button.getAttribute('data-bs-alias')

    // If necessary, you could initiate an AJAX request here
    // and then do the updating in a callback.
    
    // Update the modal's content.
    var modalTextAlias = removeModal.querySelector('.modal-body b')

    modalTextAlias.textContent = recipient

})


const showToast = (msg) => {
    let toastBox = document.getElementById('toastBox')

    let toast = document.createElement('div')
    toast.classList.add('_toast')
    toast.innerHTML = msg

    toastBox.appendChild(toast)

    if (msg.includes('Cannot') || msg.includes('AxiosError'))
        toast.classList.add('error')
    if (msg.includes('Please'))
        toast.classList.add('invalid')

    setTimeout(() => {
        toast.remove()
    }, 4500)
}

function displayPdfContent(blob) {
    // Tạo một đối tượng FileReader để đọc dữ liệu từ Blob
    var reader = new FileReader();
    reader.onloadend = function() {
        // Lấy dữ liệu đã đọc từ Blob
        var pdfData = reader.result;

        // Hiển thị dữ liệu PDF trong một div có id là 'pdfContent'
        var pdfContentDiv = document.getElementById('pdfContent');
        pdfContentDiv.textContent = pdfData;
    };

    // Đọc dữ liệu từ Blob
    reader.readAsText(blob);
}